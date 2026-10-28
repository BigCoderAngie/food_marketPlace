package na.eat.marketplace.service;

import na.eat.marketplace.domain.AuditLog;
import na.eat.marketplace.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String actor, String action, String entityType, Long entityId, String details) {
        repo.save(new AuditLog(actor, action, entityType, entityId,
                details != null && details.length() > 500 ? details.substring(0, 500) : details));
    }
}
