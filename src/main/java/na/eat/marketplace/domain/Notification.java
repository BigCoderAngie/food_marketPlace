package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Every notification is logged here (FR-NT-04), whichever channel delivered it. */
@Entity
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User recipient;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(length = 200)
    private String link;

    /** IN_APP, WHATSAPP, SMS or EMAIL. */
    @Column(nullable = false, length = 20)
    private String channel;

    /** SENT, LOGGED (provider not configured) or FAILED. */
    @Column(nullable = false, length = 20)
    private String deliveryStatus;

    private boolean readByUser = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
