package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** FR-PY-04: amount, method, status, timestamps and reference. */
@Entity
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private CustomerOrder order;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.UNPAID;

    /** eWallet/EFT reference the customer typed in. */
    @Column(length = 80)
    private String reference;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant customerMarkedSentAt;

    private Instant confirmedAt;

    private Instant refundedAt;
}
