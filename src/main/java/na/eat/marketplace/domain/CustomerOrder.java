package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The central transactional record (SRS section 8). One customer and one seller per order. */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short public reference shown to people, e.g. EAT-7K3Q9P. */
    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.ORDER_PLACED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FulfilmentMethod fulfilment;

    /** Pickup time the customer chose (SRS change #1). */
    private LocalDateTime pickupTime;

    @Column(length = 80)
    private String deliverySuburb;

    /** Landmark-style address, e.g. "Blue gate behind Shoprite Wanaheda" (SRS change #4). */
    @Column(length = 300)
    private String deliveryLandmark;

    @Column(nullable = false, length = 30)
    private String contactPhone;

    @Column(length = 500)
    private String customerNote;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /** Seller must accept before this moment or the order expires (SRS change #3). */
    @Column(nullable = false)
    private Instant acceptBy;

    /** Longest prep time of the items, used for the ready estimate. */
    private int prepMinutes;

    @Column(length = 300)
    private String reason;

    @Column(nullable = false)
    private Instant placedAt = Instant.now();

    private Instant acceptedAt;

    private Instant closedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("at")
    private List<OrderEvent> events = new ArrayList<>();

    public Instant getReadyEstimate() {
        Instant from = acceptedAt != null ? acceptedAt : placedAt;
        return from.plusSeconds(prepMinutes * 60L);
    }

    public int getItemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
