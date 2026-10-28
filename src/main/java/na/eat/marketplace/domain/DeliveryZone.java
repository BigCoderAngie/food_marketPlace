package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** A suburb a seller delivers to, with a flat fee (SRS change #4, FR-DL-03). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "suburb"}))
@Getter
@Setter
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Seller seller;

    @Column(nullable = false, length = 80)
    private String suburb;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;
}
