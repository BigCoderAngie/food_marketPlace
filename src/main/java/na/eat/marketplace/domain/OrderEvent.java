package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Timestamped order history (FR-OR-06). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus toStatus;

    /** Free text for non-status events such as payment updates. */
    @Column(nullable = false, length = 300)
    private String description;

    @Column(length = 120)
    private String actor;

    @Column(nullable = false)
    private Instant at = Instant.now();

    public OrderEvent(CustomerOrder order, OrderStatus from, OrderStatus to, String description, String actor) {
        this.order = order;
        this.fromStatus = from;
        this.toStatus = to;
        this.description = description;
        this.actor = actor;
    }
}
