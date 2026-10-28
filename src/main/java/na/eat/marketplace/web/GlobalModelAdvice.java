package na.eat.marketplace.web;

import jakarta.servlet.http.HttpServletRequest;
import na.eat.marketplace.config.AppUserDetails;
import na.eat.marketplace.service.BusinessException;
import na.eat.marketplace.service.Cart;
import na.eat.marketplace.service.NotificationService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/** Values every page needs (who is logged in, cart size, unread notifications) and friendly rule errors. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUser currentUser;
    private final Cart cart;
    private final NotificationService notifications;

    public GlobalModelAdvice(CurrentUser currentUser, Cart cart, NotificationService notifications) {
        this.currentUser = currentUser;
        this.cart = cart;
        this.notifications = notifications;
    }

    @ModelAttribute("me")
    public AppUserDetails me() {
        return currentUser.details().orElse(null);
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return cart.getCount();
    }

    @ModelAttribute("unread")
    public long unread() {
        return currentUser.find().map(notifications::unreadCount).orElse(0L);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /** A broken business rule on a form post: go back to the page with the message shown. */
    @ExceptionHandler(BusinessException.class)
    public String businessError(BusinessException e, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", e.getMessage());
        return "redirect:" + safeReferer(request);
    }

    static String safeReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) return "/";
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            // Only same-site paths, never an open redirect.
            if (!path.startsWith("/") || path.startsWith("//")) return "/";
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
