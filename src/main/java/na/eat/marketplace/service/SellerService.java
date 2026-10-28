package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.OrderRepository;
import na.eat.marketplace.repo.ReviewRepository;
import na.eat.marketplace.repo.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;

@Service
public class SellerService {

    private final SellerRepository sellers;
    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final AuditService audit;
    private final NotificationService notifications;
    private final Clock clock;

    public SellerService(SellerRepository sellers, ReviewRepository reviews, OrderRepository orders,
                         AuditService audit, NotificationService notifications, Clock clock) {
        this.sellers = sellers;
        this.reviews = reviews;
        this.orders = orders;
        this.audit = audit;
        this.notifications = notifications;
        this.clock = clock;
    }

    public Seller forOwner(User owner) {
        return sellers.findByOwner(owner).orElseThrow(() -> new NotFoundException("No seller profile for this account"));
    }

    public Seller get(Long id) {
        return sellers.findById(id).orElseThrow(() -> new NotFoundException("Seller not found"));
    }

    /** Public storefront by slug. Unverified or suspended sellers are hidden (FR-SL-03). */
    public Seller publicBySlug(String slug) {
        Seller s = sellers.findBySlug(slug).orElseThrow(() -> new NotFoundException("Seller not found"));
        if (s.getStatus() != SellerStatus.VERIFIED) throw new NotFoundException("Seller not found");
        return s;
    }

    @Transactional
    public void updateProfile(Seller seller, SellerProfileForm form, String actor) {
        if (form.getName() == null || form.getName().isBlank()) throw new BusinessException("Enter your business name.");
        if (form.getSuburb() == null || form.getSuburb().isBlank()) throw new BusinessException("Enter your suburb.");
        if (!form.isOffersPickup() && !form.isOffersDelivery()) throw new BusinessException("Offer pickup, delivery or both.");
        if (form.getAcceptWindowMinutes() < 3 || form.getAcceptWindowMinutes() > 60) throw new BusinessException("Accept time must be between 3 and 60 minutes.");
        String phone = Phones.normalise(form.getPhone());
        if (phone != null && !Phones.isValid(phone)) throw new BusinessException("Enter a Namibian phone number, e.g. 081 234 5678.");
        String whatsapp = Phones.normalise(form.getWhatsapp());
        if (whatsapp != null && !Phones.isValid(whatsapp)) throw new BusinessException("Enter a valid WhatsApp number.");

        seller.setName(form.getName().trim());
        seller.setType(form.getType());
        seller.setDescription(blankToNull(form.getDescription()));
        seller.setPhone(phone);
        seller.setWhatsapp(whatsapp);
        seller.setTown(form.getTown() == null || form.getTown().isBlank() ? "Windhoek" : form.getTown().trim());
        seller.setSuburb(form.getSuburb().trim());
        seller.setPickupInstructions(blankToNull(form.getPickupInstructions()));
        seller.setOpeningHours(blankToNull(form.getOpeningHours()));
        seller.setPaymentInstructions(blankToNull(form.getPaymentInstructions()));
        seller.setOffersPickup(form.isOffersPickup());
        seller.setOffersDelivery(form.isOffersDelivery());
        seller.setAcceptWindowMinutes(form.getAcceptWindowMinutes());
        audit.log(actor, "SELLER_PROFILE_UPDATED", "Seller", seller.getId(), null);
    }

    @Transactional
    public void addZone(Seller seller, String suburb, BigDecimal fee, String actor) {
        if (suburb == null || suburb.isBlank()) throw new BusinessException("Enter a suburb.");
        if (fee == null || fee.signum() < 0) throw new BusinessException("Enter a delivery fee of 0 or more.");
        boolean exists = seller.getDeliveryZones().stream().anyMatch(z -> z.getSuburb().equalsIgnoreCase(suburb.trim()));
        if (exists) throw new BusinessException("You already deliver to " + suburb.trim() + ".");
        DeliveryZone zone = new DeliveryZone();
        zone.setSeller(seller);
        zone.setSuburb(suburb.trim());
        zone.setFee(fee.setScale(2, RoundingMode.HALF_UP));
        seller.getDeliveryZones().add(zone);
        audit.log(actor, "DELIVERY_ZONE_ADDED", "Seller", seller.getId(), suburb + " " + fee);
    }

    @Transactional
    public void removeZone(Seller seller, Long zoneId, String actor) {
        seller.getDeliveryZones().removeIf(z -> z.getId().equals(zoneId));
        audit.log(actor, "DELIVERY_ZONE_REMOVED", "Seller", seller.getId(), String.valueOf(zoneId));
    }

    @Transactional
    public void setPaused(Seller seller, boolean paused, String actor) {
        seller.setPaused(paused);
        audit.log(actor, paused ? "STOREFRONT_PAUSED" : "STOREFRONT_RESUMED", "Seller", seller.getId(), null);
    }

    // --- Admin verification (FR-AC-04, FR-AD-02) ---

    @Transactional
    public void verify(Long sellerId, String actor) {
        Seller s = get(sellerId);
        s.setStatus(SellerStatus.VERIFIED);
        s.setStatusNote(null);
        s.setVerifiedAt(clock.instant());
        s.getOwner().setStatus(AccountStatus.ACTIVE);
        audit.log(actor, "SELLER_VERIFIED", "Seller", s.getId(), s.getName());
        notifications.notify(s.getOwner(), "Your storefront " + s.getName() + " is verified and now visible to customers.", "/seller");
    }

    @Transactional
    public void reject(Long sellerId, String reason, String actor) {
        Seller s = get(sellerId);
        requireReason(reason);
        s.setStatus(SellerStatus.REJECTED);
        s.setStatusNote(reason.trim());
        audit.log(actor, "SELLER_REJECTED", "Seller", s.getId(), reason);
        notifications.notify(s.getOwner(), "Your seller application was not approved: " + reason.trim(), "/seller");
    }

    @Transactional
    public void suspend(Long sellerId, String reason, String actor) {
        Seller s = get(sellerId);
        requireReason(reason);
        s.setStatus(SellerStatus.SUSPENDED);
        s.setStatusNote(reason.trim());
        s.getOwner().setStatus(AccountStatus.SUSPENDED);
        audit.log(actor, "SELLER_SUSPENDED", "Seller", s.getId(), reason);
    }

    @Transactional
    public void reinstate(Long sellerId, String actor) {
        Seller s = get(sellerId);
        s.setStatus(SellerStatus.VERIFIED);
        s.setStatusNote(null);
        s.getOwner().setStatus(AccountStatus.ACTIVE);
        audit.log(actor, "SELLER_REINSTATED", "Seller", s.getId(), s.getName());
        notifications.notify(s.getOwner(), "Your storefront has been reinstated.", "/seller");
    }

    /** Recompute the public rating and completed-order count (FR-RV-05). */
    @Transactional
    public void refreshStats(Seller seller) {
        List<Object[]> agg = reviews.aggregate(seller);
        Object[] row = agg.isEmpty() ? new Object[]{null, 0L} : agg.getFirst();
        Double avg = (Double) row[0];
        long count = ((Number) row[1]).longValue();
        seller.setRatingAverage(avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        seller.setRatingCount((int) count);
        seller.setCompletedOrders(orders.countCompleted(seller));
    }

    public List<Seller> pendingVerification() {
        return sellers.findByStatusOrderByCreatedAtAsc(SellerStatus.PENDING_REVIEW);
    }

    public List<Seller> all() {
        return sellers.findAllByOrderByCreatedAtDesc();
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("Give a reason.");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
