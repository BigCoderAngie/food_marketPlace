package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reviews only for completed orders, one per order (FR-RV-01..05). */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderService orders;
    private final SellerService sellers;
    private final NotificationService notifications;
    private final AuditService audit;

    public ReviewService(ReviewRepository reviews, OrderService orders, SellerService sellers,
                         NotificationService notifications, AuditService audit) {
        this.reviews = reviews;
        this.orders = orders;
        this.sellers = sellers;
        this.notifications = notifications;
        this.audit = audit;
    }

    public boolean canReview(CustomerOrder o) {
        return o.getStatus() == OrderStatus.COMPLETED && !reviews.existsByOrder(o);
    }

    public Review forOrder(CustomerOrder o) {
        return reviews.findByOrder(o).orElse(null);
    }

    @Transactional
    public void submit(User customer, String code, int rating, String comment) {
        CustomerOrder o = orders.forCustomer(customer, code);
        if (o.getStatus() != OrderStatus.COMPLETED) throw new BusinessException("You can review an order once it's completed.");
        if (reviews.existsByOrder(o)) throw new BusinessException("You already reviewed this order.");
        if (rating < 1 || rating > 5) throw new BusinessException("Choose 1 to 5 stars.");
        Review r = new Review();
        r.setOrder(o);
        r.setAuthor(customer);
        r.setSeller(o.getSeller());
        r.setRating(rating);
        r.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        reviews.save(r);
        reviews.flush();
        sellers.refreshStats(o.getSeller());
        notifications.notify(o.getSeller().getOwner(), customer.getName() + " left a " + rating + "-star review.", "/seller/reviews");
    }

    public List<Review> publicForSeller(Seller seller) {
        return reviews.findBySellerAndHiddenFalseOrderByCreatedAtDesc(seller);
    }

    public List<Review> allForSeller(Seller seller) {
        return reviews.findBySellerOrderByCreatedAtDesc(seller);
    }

    public List<Review> all() {
        return reviews.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void setHidden(Long reviewId, boolean hidden, String admin) {
        Review r = reviews.findById(reviewId).orElseThrow(() -> new NotFoundException("Review not found"));
        r.setHidden(hidden);
        reviews.flush();
        sellers.refreshStats(r.getSeller());
        audit.log(admin, hidden ? "REVIEW_HIDDEN" : "REVIEW_SHOWN", "Review", r.getId(), null);
    }
}
