package na.eat.marketplace.repo;

import na.eat.marketplace.domain.Notification;
import na.eat.marketplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipientAndChannelOrderByCreatedAtDesc(User recipient, String channel);

    long countByRecipientAndChannelAndReadByUserFalse(User recipient, String channel);

    List<Notification> findTop100ByOrderByCreatedAtDesc();

    @Modifying
    @Query("update Notification n set n.readByUser = true where n.recipient = :user and n.readByUser = false")
    int markAllRead(@Param("user") User user);
}
