package na.eat.marketplace.web;

import na.eat.marketplace.domain.User;
import na.eat.marketplace.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    private final NotificationService notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping("/notifications")
    public String inbox(Model model) {
        User me = currentUser.get();
        model.addAttribute("items", notifications.inbox(me));
        notifications.markAllRead(me);
        model.addAttribute("unread", 0L);
        return "notifications";
    }
}
