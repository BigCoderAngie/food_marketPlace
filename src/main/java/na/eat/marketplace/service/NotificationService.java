package na.eat.marketplace.service;

import na.eat.marketplace.domain.Notification;
import na.eat.marketplace.domain.User;
import na.eat.marketplace.repo.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * In-app notifications are the baseline (FR-NT-03). WhatsApp/SMS are logged as "LOGGED"
 * until a provider is connected, so the hook is ready and every attempt is recorded (FR-NT-04).
 */
@Service
public class NotificationService {

    public static final String IN_APP = "IN_APP";
    public static final String WHATSAPP = "WHATSAPP";

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final String baseUrl;
    private final boolean whatsappEnabled;

    public NotificationService(NotificationRepository repo,
                               @Value("${eatna.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${eatna.notifications.whatsapp-enabled:false}") boolean whatsappEnabled) {
        this.repo = repo;
        this.baseUrl = baseUrl;
        this.whatsappEnabled = whatsappEnabled;
    }

    @Transactional
    public void notify(User recipient, String message, String link) {
        save(recipient, message, link, IN_APP, "SENT");
        if (recipient.getPhone() != null) {
            // Placeholder for a WhatsApp Business / SMS provider. The full link lets a seller
            // tap straight through to Accept (SRS change #2).
            String text = message + (link != null ? " " + baseUrl + link : "");
            if (whatsappEnabled) {
                log.info("WhatsApp to {}: {}", recipient.getPhone(), text);
            }
            save(recipient, text, link, WHATSAPP, whatsappEnabled ? "SENT" : "LOGGED");
        }
    }

    private void save(User recipient, String message, String link, String channel, String status) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setMessage(message.length() > 300 ? message.substring(0, 300) : message);
        n.setLink(link);
        n.setChannel(channel);
        n.setDeliveryStatus(status);
        repo.save(n);
    }

    public List<Notification> inbox(User user) {
        return repo.findTop50ByRecipientAndChannelOrderByCreatedAtDesc(user, IN_APP);
    }

    public long unreadCount(User user) {
        return repo.countByRecipientAndChannelAndReadByUserFalse(user, IN_APP);
    }

    @Transactional
    public void markAllRead(User user) {
        repo.markAllRead(user);
    }
}
