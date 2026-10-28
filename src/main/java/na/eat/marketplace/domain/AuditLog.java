package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Sensitive actions are auditable (SRS 8.2, 13.3). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 160)
    private String actor;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 40)
    private String entityType;

    private Long entityId;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private Instant at = Instant.now();

    public AuditLog(String actor, String action, String entityType, Long entityId, String details) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }
}
