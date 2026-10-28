package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.repo.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The MVP loop from SRS 15.1, plus the rules around it. */
@SpringBootTest
@Transactional
class OrderFlowTest {

    private static final AtomicInteger SEQ = new AtomicInteger(10);

    @Autowired AccountService accounts;
    @Autowired SellerService sellers;
    @Autowired OrderService orders;
    @Autowired ReviewService reviews;
    @Autowired SearchService search;
    @Autowired FoodItemRepository foods;
    @Autowired OrderRepository orderRepo;

    Seller seller;
    User customer;
    FoodItem stew;

    @BeforeEach
    void setUp() {
        int n = SEQ.incrementAndGet();
        seller = accounts.registerSeller("Cook " + n, null, "08100000" + n, "password1",
                "Kitchen " + n, SellerType.HOME_COOK, "Katutura", null);
        sellers.addZone(seller, "Wanaheda", new BigDecimal("20"), "test");
        seller.setOffersDelivery(true);
        customer = accounts.registerCustomer("Customer " + n, "c" + n + "@test.na", null, "password1");
        stew = new FoodItem();
        stew.setSeller(seller);
        stew.setName("Beef stew");
        stew.setPrice(new BigDecimal("55.00"));
        stew.setPrepMinutes(15);
        stew.setDailyPortions(3);
        stew.setPortionsDate(LocalDate.now());
        foods.save(stew);
    }

    private CustomerOrder order(int qty) {
        Cart cart = new Cart();
        cart.put(seller.getId(), stew.getId(), qty);
        CheckoutForm form = new CheckoutForm();
        form.setFulfilment(FulfilmentMethod.PICKUP);
        form.setPickupTime(orders.earliestPickup(stew.getPrepMinutes()));
        form.setContactPhone("081 999 0000");
        return orders.place(customer, cart, form);
    }

    @Test
    void unverifiedSellersCannotTakeOrders() {
        assertThatThrownBy(() -> order(1)).isInstanceOf(BusinessException.class).hasMessageContaining("isn't taking orders");
    }

    @Test
    void fullLoopFromOrderToReview() {
        sellers.verify(seller.getId(), "admin");
        CustomerOrder o = order(2);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.ORDER_PLACED);
        assertThat(o.getTotal()).isEqualByComparingTo("110.00");
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(stew.portionsLeft(LocalDate.now())).isEqualTo(1);

        // Not completed yet, so no review.
        assertThatThrownBy(() -> reviews.submit(customer, o.getCode(), 5, null)).isInstanceOf(BusinessException.class);

        orders.accept(seller, o.getCode());
        orders.startPreparing(seller, o.getCode());
        orders.markReady(seller, o.getCode());
        orders.handOver(seller, o.getCode());
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        orders.confirmReceived(customer, o.getCode());
        assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        // Cash is recorded as paid on completion.
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.PAID);

        reviews.submit(customer, o.getCode(), 4, "Good");
        assertThat(seller.getRatingCount()).isEqualTo(1);
        assertThat(seller.getCompletedOrders()).isEqualTo(1);
        assertThatThrownBy(() -> reviews.submit(customer, o.getCode(), 5, null)).hasMessageContaining("already reviewed");
        assertThat(o.getEvents()).extracting(OrderEvent::getToStatus).contains(
                OrderStatus.ORDER_PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.PICKED_UP, OrderStatus.COMPLETED);
    }

    @Test
    void portionsCannotBeOversoldAndComeBackOnReject() {
        sellers.verify(seller.getId(), "admin");
        CustomerOrder first = order(3);
        assertThat(stew.getStatus()).isEqualTo(FoodStatus.SOLD_OUT);
        assertThatThrownBy(() -> order(1)).isInstanceOf(BusinessException.class);

        orders.reject(seller, first.getCode(), "Gas ran out");
        assertThat(stew.portionsLeft(LocalDate.now())).isEqualTo(3);
        assertThat(stew.getStatus()).isEqualTo(FoodStatus.AVAILABLE);
    }

    @Test
    void invalidTransitionsAreRefused() {
        sellers.verify(seller.getId(), "admin");
        CustomerOrder o = order(1);
        assertThatThrownBy(() -> orders.markReady(seller, o.getCode())).hasMessageContaining("can't be moved");
        assertThatThrownBy(() -> orders.reject(seller, o.getCode(), " ")).hasMessageContaining("why");
        orders.accept(seller, o.getCode());
        assertThatThrownBy(() -> orders.cancelByCustomer(customer, o.getCode(), "changed my mind"))
                .hasMessageContaining("already accepted");
    }

    @Test
    void unansweredOrdersExpireAndSuggestOtherSellers() {
        sellers.verify(seller.getId(), "admin");
        CustomerOrder o = order(1);
        o.setAcceptBy(Instant.now().minusSeconds(5));
        orderRepo.flush();
        assertThat(orders.expireOverdue()).isGreaterThanOrEqualTo(1);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(stew.portionsLeft(LocalDate.now())).isEqualTo(3);
        assertThat(search.similarSellers(o, 3)).noneMatch(s -> s.getId().equals(seller.getId()));
    }

    @Test
    void ewalletPaymentNeedsSellerConfirmationAndRefundIfCancelled() {
        sellers.verify(seller.getId(), "admin");
        Cart cart = new Cart();
        cart.put(seller.getId(), stew.getId(), 1);
        CheckoutForm form = new CheckoutForm();
        form.setFulfilment(FulfilmentMethod.DELIVERY);
        form.setDeliverySuburb("Wanaheda");
        form.setDeliveryLandmark("Blue gate near the clinic");
        form.setContactPhone("081 999 0000");
        form.setPaymentMethod(PaymentMethod.EWALLET);
        CustomerOrder o = orders.place(customer, cart, form);
        assertThat(o.getTotal()).isEqualByComparingTo("75.00");

        orders.accept(seller, o.getCode());
        orders.markPaymentSent(customer, o.getCode(), "BW123");
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.AWAITING_CONFIRMATION);
        orders.confirmPayment(seller, o.getCode());
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.PAID);

        orders.cancelBySeller(seller, o.getCode(), "Driver unavailable");
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUND_DUE);
        orders.markRefunded(seller, o.getCode());
        assertThat(o.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void deliveryOnlyToListedSuburbs() {
        sellers.verify(seller.getId(), "admin");
        Cart cart = new Cart();
        cart.put(seller.getId(), stew.getId(), 1);
        CheckoutForm form = new CheckoutForm();
        form.setFulfilment(FulfilmentMethod.DELIVERY);
        form.setDeliverySuburb("Swakopmund");
        form.setDeliveryLandmark("Near the lighthouse");
        form.setContactPhone("081 999 0000");
        assertThatThrownBy(() -> orders.place(customer, cart, form)).hasMessageContaining("doesn't deliver to");
    }

    @Test
    void sellersCannotTouchOtherSellersOrders() {
        sellers.verify(seller.getId(), "admin");
        CustomerOrder o = order(1);
        Seller other = accounts.registerSeller("Other", null, "0812345000", "password1", "Other kitchen",
                SellerType.BAKERY, "Eros", null);
        assertThatThrownBy(() -> orders.accept(other, o.getCode())).isInstanceOf(NotFoundException.class);
    }
}
