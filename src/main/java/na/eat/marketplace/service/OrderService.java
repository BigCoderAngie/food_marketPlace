package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.DeliveryZoneRepository;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static na.eat.marketplace.domain.OrderStatus.*;

/**
 * Order placement and the order workflow. Every status change goes through {@link #move}, which
 * enforces the state model (FR-OR-08), records an event (FR-OR-06), keeps portions and payment in
 * step, and notifies the other side (FR-NT-01, FR-NT-02).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Customers may pick a pickup time up to this far ahead. */
    private static final Duration MAX_PICKUP_AHEAD = Duration.ofDays(3);

    private final OrderRepository orders;
    private final FoodItemRepository foods;
    private final DeliveryZoneRepository zones;
    private final NotificationService notifications;
    private final SellerService sellerService;
    private final AuditService audit;
    private final Clock clock;

    public OrderService(OrderRepository orders, FoodItemRepository foods, DeliveryZoneRepository zones,
                        NotificationService notifications, SellerService sellerService, AuditService audit, Clock clock) {
        this.orders = orders;
        this.foods = foods;
        this.zones = zones;
        this.notifications = notifications;
        this.sellerService = sellerService;
        this.audit = audit;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ placing

    /** Earliest pickup time: now plus the longest prep time, rounded up to the next 15 minutes. */
    public LocalDateTime earliestPickup(int prepMinutes) {
        LocalDateTime t = LocalDateTime.now(clock).plusMinutes(Math.max(prepMinutes, 10));
        int add = (15 - t.getMinute() % 15) % 15;
        return t.plusMinutes(add).withSecond(0).withNano(0);
    }

    public List<LocalDateTime> pickupSlots(int prepMinutes) {
        LocalDateTime first = earliestPickup(prepMinutes);
        List<LocalDateTime> slots = new ArrayList<>();
        for (int i = 0; i < 32; i++) slots.add(first.plusMinutes(15L * i));
        return slots;
    }

    @Transactional
    public CustomerOrder place(User customer, Cart cart, CheckoutForm form) {
        if (cart.isEmpty()) throw new BusinessException("Your cart is empty.");
        if (customer.getRole() != Role.CUSTOMER) throw new BusinessException("Sign in with a customer account to order.");
        Seller seller = sellerService.get(cart.getSellerId());
        if (!seller.isOpenForOrders()) throw new BusinessException(seller.getName() + " isn't taking orders right now.");

        String phone = Phones.normalise(form.getContactPhone());
        if (!Phones.isValid(phone)) throw new BusinessException("Enter a phone number the seller can call, e.g. 081 234 5678.");

        // Lock the food rows and re-check availability (FR-CT-04, SRS 5.1).
        Map<Long, Integer> wanted = new LinkedHashMap<>(cart.getQuantities());
        List<FoodItem> items = foods.lockByIds(wanted.keySet());
        if (items.size() != wanted.size()) throw new BusinessException("Some items in your cart are no longer on the menu.");
        LocalDate today = LocalDate.now(clock);
        int maxPrep = 0;
        for (FoodItem f : items) {
            if (!f.getSeller().getId().equals(seller.getId())) throw new BusinessException("A cart can only hold food from one seller.");
            int qty = wanted.get(f.getId());
            if (qty < 1) throw new BusinessException("Invalid quantity for " + f.getName() + ".");
            if (!f.isOrderable(today)) throw new BusinessException(f.getName() + " just sold out or became unavailable.");
            Integer left = f.portionsLeft(today);
            if (left != null && qty > left) {
                throw new BusinessException("Only " + left + " portion" + (left == 1 ? "" : "s") + " of " + f.getName() + " left.");
            }
            maxPrep = Math.max(maxPrep, f.getPrepMinutes());
        }

        CustomerOrder o = new CustomerOrder();
        o.setCode(newCode());
        o.setCustomer(customer);
        o.setSeller(seller);
        o.setContactPhone(phone);
        o.setCustomerNote(blankToNull(form.getNote()));
        o.setPrepMinutes(maxPrep);
        o.setFulfilment(form.getFulfilment());

        BigDecimal fee = BigDecimal.ZERO;
        if (form.getFulfilment() == FulfilmentMethod.DELIVERY) {
            if (!seller.isOffersDelivery()) throw new BusinessException(seller.getName() + " doesn't deliver. Choose pickup.");
            if (form.getDeliverySuburb() == null || form.getDeliverySuburb().isBlank()) throw new BusinessException("Choose your suburb.");
            DeliveryZone zone = zones.findBySellerAndSuburbIgnoreCase(seller, form.getDeliverySuburb().trim())
                    .orElseThrow(() -> new BusinessException(seller.getName() + " doesn't deliver to " + form.getDeliverySuburb() + "."));
            if (form.getDeliveryLandmark() == null || form.getDeliveryLandmark().trim().length() < 5) {
                throw new BusinessException("Describe where to deliver, e.g. \"Blue gate opposite Wanaheda clinic\".");
            }
            fee = zone.getFee();
            o.setDeliverySuburb(zone.getSuburb());
            o.setDeliveryLandmark(form.getDeliveryLandmark().trim());
        } else if (form.getFulfilment() == FulfilmentMethod.PICKUP) {
            if (!seller.isOffersPickup()) throw new BusinessException(seller.getName() + " only delivers. Choose delivery.");
            LocalDateTime t = form.getPickupTime();
            if (t == null) throw new BusinessException("Choose a pickup time.");
            LocalDateTime now = LocalDateTime.now(clock);
            // Small grace so a slot picked a few minutes ago is still accepted.
            if (t.isBefore(now.plusMinutes(maxPrep).minusMinutes(10))) {
                throw new BusinessException("That pickup time is too soon. The food needs about " + maxPrep + " minutes.");
            }
            if (t.isAfter(now.plus(MAX_PICKUP_AHEAD))) throw new BusinessException("Pickup can be at most 3 days ahead.");
            o.setPickupTime(t);
        } else {
            throw new BusinessException("Choose pickup or delivery.");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (FoodItem f : items) {
            int qty = wanted.get(f.getId());
            OrderItem line = new OrderItem();
            line.setOrder(o);
            line.setFoodItem(f);
            line.setName(f.getName());
            line.setUnitPrice(f.getPrice());   // price snapshot (SRS 8.2)
            line.setQuantity(qty);
            o.getItems().add(line);
            subtotal = subtotal.add(line.getLineTotal());
            reservePortions(f, qty, today);
        }
        o.setSubtotal(subtotal);
        o.setDeliveryFee(fee);
        o.setTotal(subtotal.add(fee));
        Instant now = clock.instant();
        o.setPlacedAt(now);
        o.setAcceptBy(now.plusSeconds(seller.getAcceptWindowMinutes() * 60L));

        Payment p = new Payment();
        p.setOrder(o);
        p.setAmount(o.getTotal());
        p.setMethod(form.getPaymentMethod() == null ? PaymentMethod.CASH : form.getPaymentMethod());
        p.setStatus(PaymentStatus.UNPAID);
        o.setPayment(p);

        o.getEvents().add(new OrderEvent(o, null, ORDER_PLACED, "Order placed", customer.getName()));
        orders.save(o);
        cart.clear();

        notifications.notify(seller.getOwner(),
                "New order " + o.getCode() + " (" + money(o.getTotal()) + "). Accept within "
                        + seller.getAcceptWindowMinutes() + " min.",
                "/seller/orders/" + o.getCode());
        return o;
    }

    // ------------------------------------------------------------------ seller actions

    /** Not rolled back on a rule error, so an order found overdue here stays expired. */
    @Transactional(noRollbackFor = BusinessException.class)
    public void accept(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        if (o.getStatus() == ORDER_PLACED && clock.instant().isAfter(o.getAcceptBy())) {
            expire(o);
            throw new BusinessException("Too late: this order expired before it was accepted.");
        }
        move(o, ACCEPTED, seller.getName(), null);
        o.setAcceptedAt(clock.instant());
        notifications.notify(o.getCustomer(),
                seller.getName() + " accepted order " + o.getCode() + ". Ready around " + time(o.getReadyEstimate()) + ".",
                "/orders/" + o.getCode());
    }

    @Transactional
    public void reject(Seller seller, String code, String reason) {
        CustomerOrder o = forSeller(seller, code);
        requireReason(reason, "Tell the customer why you can't take this order.");
        move(o, REJECTED, seller.getName(), reason);
        notifications.notify(o.getCustomer(),
                seller.getName() + " can't take order " + o.getCode() + ": " + reason.trim() + ". See similar sellers nearby.",
                "/orders/" + o.getCode());
    }

    @Transactional
    public void startPreparing(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        move(o, PREPARING, seller.getName(), null);
        notifications.notify(o.getCustomer(), "Order " + o.getCode() + " is being prepared.", "/orders/" + o.getCode());
    }

    @Transactional
    public void markReady(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        move(o, READY, seller.getName(), null);
        String msg = o.getFulfilment() == FulfilmentMethod.PICKUP
                ? "Order " + o.getCode() + " is ready for pickup at " + seller.getName() + "."
                : "Order " + o.getCode() + " is ready and will be on its way soon.";
        notifications.notify(o.getCustomer(), msg, "/orders/" + o.getCode());
    }

    @Transactional
    public void handOver(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        if (o.getFulfilment() == FulfilmentMethod.PICKUP) {
            move(o, PICKED_UP, seller.getName(), null);
        } else {
            move(o, OUT_FOR_DELIVERY, seller.getName(), null);
            notifications.notify(o.getCustomer(), "Order " + o.getCode() + " is out for delivery.", "/orders/" + o.getCode());
        }
    }

    @Transactional
    public void markDelivered(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        move(o, DELIVERED, seller.getName(), null);
        notifications.notify(o.getCustomer(), "Order " + o.getCode() + " was delivered. Please confirm you received it.", "/orders/" + o.getCode());
    }

    @Transactional
    public void reportDeliveryProblem(Seller seller, String code, String reason) {
        CustomerOrder o = forSeller(seller, code);
        requireReason(reason, "Describe the delivery problem.");
        move(o, DELIVERY_PROBLEM, seller.getName(), reason);
        notifications.notify(o.getCustomer(), "There is a problem delivering order " + o.getCode() + ": " + reason.trim(), "/orders/" + o.getCode());
    }

    @Transactional
    public void completeBySeller(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        complete(o, seller.getName());
    }

    @Transactional
    public void cancelBySeller(Seller seller, String code, String reason) {
        CustomerOrder o = forSeller(seller, code);
        requireReason(reason, "Give a reason for cancelling (FR-OR-07).");
        move(o, CANCELLED, seller.getName(), reason);
        notifications.notify(o.getCustomer(), seller.getName() + " cancelled order " + o.getCode() + ": " + reason.trim(), "/orders/" + o.getCode());
    }

    /** FR-PY-05: only the seller's confirmation marks an order as paid. */
    @Transactional
    public void confirmPayment(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        Payment p = o.getPayment();
        if (p.getStatus() != PaymentStatus.UNPAID && p.getStatus() != PaymentStatus.AWAITING_CONFIRMATION) {
            throw new BusinessException("Payment is already " + p.getStatus().getLabel().toLowerCase() + ".");
        }
        if (o.getStatus().isFailed()) throw new BusinessException("This order is closed.");
        setPayment(o, PaymentStatus.PAID, seller.getName(), "Seller confirmed payment received");
        p.setConfirmedAt(clock.instant());
        notifications.notify(o.getCustomer(), seller.getName() + " confirmed your payment for order " + o.getCode() + ".", "/orders/" + o.getCode());
    }

    @Transactional
    public void markRefunded(Seller seller, String code) {
        CustomerOrder o = forSeller(seller, code);
        if (o.getPayment().getStatus() != PaymentStatus.REFUND_DUE) throw new BusinessException("No refund is due on this order.");
        setPayment(o, PaymentStatus.REFUNDED, seller.getName(), "Seller marked the refund as sent");
        o.getPayment().setRefundedAt(clock.instant());
        notifications.notify(o.getCustomer(), seller.getName() + " says your refund for order " + o.getCode() + " has been sent.", "/orders/" + o.getCode());
    }

    // ------------------------------------------------------------------ customer actions

    @Transactional
    public void cancelByCustomer(User customer, String code, String reason) {
        CustomerOrder o = forCustomer(customer, code);
        if (o.getStatus() != ORDER_PLACED) throw new BusinessException("The seller already accepted this order. Contact them to cancel.");
        requireReason(reason, "Tell the seller why you're cancelling.");
        move(o, CANCELLED, customer.getName(), reason);
        notifications.notify(o.getSeller().getOwner(), "Order " + o.getCode() + " was cancelled by the customer: " + reason.trim(), "/seller/orders/" + o.getCode());
    }

    @Transactional
    public void confirmReceived(User customer, String code) {
        CustomerOrder o = forCustomer(customer, code);
        complete(o, customer.getName());
        notifications.notify(o.getSeller().getOwner(), "The customer confirmed they received order " + o.getCode() + ".", "/seller/orders/" + o.getCode());
    }

    /** Customer says they sent an eWallet/EFT payment; the seller still has to confirm it. */
    @Transactional
    public void markPaymentSent(User customer, String code, String reference) {
        CustomerOrder o = forCustomer(customer, code);
        Payment p = o.getPayment();
        if (!p.getMethod().isTransfer()) throw new BusinessException("This order is paid in cash.");
        if (p.getStatus() != PaymentStatus.UNPAID) throw new BusinessException("Payment is already " + p.getStatus().getLabel().toLowerCase() + ".");
        if (o.getStatus().isFailed()) throw new BusinessException("This order is closed. Don't send money for it.");
        p.setReference(blankToNull(reference));
        p.setCustomerMarkedSentAt(clock.instant());
        setPayment(o, PaymentStatus.AWAITING_CONFIRMATION, customer.getName(),
                "Customer says payment was sent" + (p.getReference() != null ? " (ref " + p.getReference() + ")" : ""));
        notifications.notify(o.getSeller().getOwner(),
                "Customer says they paid " + money(p.getAmount()) + " for order " + o.getCode() + ". Check and confirm.",
                "/seller/orders/" + o.getCode());
    }

    // ------------------------------------------------------------------ admin

    @Transactional
    public void adminResolve(String code, OrderStatus target, String note, String admin) {
        CustomerOrder o = byCode(code);
        if (o.getStatus() != DELIVERY_PROBLEM) throw new BusinessException("Only orders with a delivery problem can be resolved here.");
        requireReason(note, "Add a note describing the outcome.");
        if (target == COMPLETED) complete(o, admin + " (admin)");
        else move(o, target, admin + " (admin)", note);
        audit.log(admin, "ORDER_RESOLVED", "Order", o.getId(), target + ": " + note);
    }

    // ------------------------------------------------------------------ expiry

    /** Called by the scheduler: orders nobody accepted in time become EXPIRED (SRS change #3). */
    @Transactional
    public int expireOverdue() {
        List<CustomerOrder> overdue = orders.findByStatusAndAcceptByBefore(ORDER_PLACED, clock.instant());
        overdue.forEach(this::expire);
        if (!overdue.isEmpty()) log.info("Expired {} order(s)", overdue.size());
        return overdue.size();
    }

    private void expire(CustomerOrder o) {
        move(o, EXPIRED, "System", "Seller did not respond in time");
        notifications.notify(o.getCustomer(),
                o.getSeller().getName() + " didn't respond to order " + o.getCode() + " in time. See similar sellers nearby.",
                "/orders/" + o.getCode());
        notifications.notify(o.getSeller().getOwner(), "Order " + o.getCode() + " expired because it wasn't accepted in time.",
                "/seller/orders/" + o.getCode());
    }

    // ------------------------------------------------------------------ core

    private void complete(CustomerOrder o, String actor) {
        move(o, COMPLETED, actor, null);
        if (o.getPayment().getMethod() == PaymentMethod.CASH && o.getPayment().getStatus() == PaymentStatus.UNPAID) {
            // Cash changes hands at handover; completing the order records it.
            setPayment(o, PaymentStatus.PAID, actor, "Cash received on completion");
            o.getPayment().setConfirmedAt(clock.instant());
        }
        sellerService.refreshStats(o.getSeller());
    }

    /** The only place an order's status changes. */
    void move(CustomerOrder o, OrderStatus to, String actor, String reason) {
        OrderStatus from = o.getStatus();
        if (!from.canMoveTo(to)) {
            throw new BusinessException("This order is " + from.getLabel().toLowerCase() + " and can't be moved to " + to.getLabel().toLowerCase() + ".");
        }
        o.setStatus(to);
        if (reason != null) o.setReason(reason.trim());
        String text = to.getLabel() + (reason != null && !reason.isBlank() ? ": " + reason.trim() : "");
        o.getEvents().add(new OrderEvent(o, from, to, text, actor));
        if (to.isFinal()) o.setClosedAt(clock.instant());

        if (to.isFailed()) {
            if (from == ORDER_PLACED || from == ACCEPTED || from == PREPARING || from == READY) releasePortions(o);
            PaymentStatus ps = o.getPayment().getStatus();
            if (ps == PaymentStatus.PAID || ps == PaymentStatus.AWAITING_CONFIRMATION) {
                setPayment(o, PaymentStatus.REFUND_DUE, "System", "Order " + to.getLabel().toLowerCase() + "; money must be returned");
            }
        }
        audit.log(actor, "ORDER_STATUS", "Order", o.getId(), from + " -> " + to);
    }

    private void setPayment(CustomerOrder o, PaymentStatus status, String actor, String text) {
        PaymentStatus before = o.getPayment().getStatus();
        o.getPayment().setStatus(status);
        o.getEvents().add(new OrderEvent(o, null, null, "Payment: " + text, actor));
        audit.log(actor, "PAYMENT_STATUS", "Order", o.getId(), before + " -> " + status);
    }

    private void reservePortions(FoodItem f, int qty, LocalDate today) {
        if (f.getDailyPortions() == null) return;
        if (!today.equals(f.getPortionsDate())) {
            f.setPortionsDate(today);
            f.setPortionsSold(0);
        }
        f.setPortionsSold(f.getPortionsSold() + qty);
        if (f.getPortionsSold() >= f.getDailyPortions()) f.setStatus(FoodStatus.SOLD_OUT);
    }

    /** Give portions back when an order fails the same day it was placed. */
    private void releasePortions(CustomerOrder o) {
        LocalDate today = LocalDate.now(clock);
        LocalDate placedDay = LocalDate.ofInstant(o.getPlacedAt(), clock.getZone());
        if (!today.equals(placedDay)) return;
        Map<Long, Integer> qty = o.getItems().stream()
                .collect(Collectors.toMap(i -> i.getFoodItem().getId(), OrderItem::getQuantity, Integer::sum));
        for (FoodItem f : foods.lockByIds(qty.keySet())) {
            if (f.getDailyPortions() == null || !today.equals(f.getPortionsDate())) continue;
            f.setPortionsSold(Math.max(0, f.getPortionsSold() - qty.get(f.getId())));
            if (f.getStatus() == FoodStatus.SOLD_OUT && f.getPortionsSold() < f.getDailyPortions()) f.setStatus(FoodStatus.AVAILABLE);
        }
    }

    // ------------------------------------------------------------------ lookups

    public CustomerOrder byCode(String code) {
        return orders.findByCode(code).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    public CustomerOrder forSeller(Seller seller, String code) {
        CustomerOrder o = byCode(code);
        if (!o.getSeller().getId().equals(seller.getId())) throw new NotFoundException("Order not found");
        return o;
    }

    public CustomerOrder forCustomer(User customer, String code) {
        CustomerOrder o = byCode(code);
        if (!o.getCustomer().getId().equals(customer.getId())) throw new NotFoundException("Order not found");
        return o;
    }

    public List<CustomerOrder> customerOrders(User customer) {
        return orders.findByCustomerOrderByPlacedAtDesc(customer);
    }

    public List<CustomerOrder> activeForSeller(Seller seller) {
        return orders.findBySellerAndStatusInOrderByPlacedAtAsc(seller,
                EnumSet.of(ORDER_PLACED, ACCEPTED, PREPARING, READY, OUT_FOR_DELIVERY, PICKED_UP, DELIVERED, DELIVERY_PROBLEM));
    }

    public List<CustomerOrder> allForSeller(Seller seller) {
        return orders.findBySellerOrderByPlacedAtDesc(seller);
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("EAT-");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
            code = sb.toString();
        } while (orders.existsByCode(code));
        return code;
    }

    private String money(BigDecimal amount) {
        return "N$ " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String time(Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(instant.atZone(clock.getZone()));
    }

    private static void requireReason(String reason, String message) {
        if (reason == null || reason.isBlank()) throw new BusinessException(message);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
