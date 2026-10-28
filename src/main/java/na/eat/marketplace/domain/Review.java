package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One review per completed order (FR-RV-01..03). */
@Entity
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private CustomerOrder order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User author;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Seller seller;

    private int rating;

    @Column(length = 1000)
    private String comment;

    /** FR-RV-04: hidden by an admin. */
    private boolean hidden = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
