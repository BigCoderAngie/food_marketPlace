package na.eat.marketplace.web.admin;

import na.eat.marketplace.domain.OrderStatus;
import na.eat.marketplace.repo.AuditLogRepository;
import na.eat.marketplace.repo.NotificationRepository;
import na.eat.marketplace.repo.OrderRepository;
import na.eat.marketplace.repo.UserRepository;
import na.eat.marketplace.service.*;
import na.eat.marketplace.web.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin website (SRS 7.3, section 13). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final SellerService sellers;
    private final OrderService orders;
    private final ReviewService reviews;
    private final MenuService menus;
    private final ReportService reports;
    private final OrderRepository orderRepo;
    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final NotificationRepository notificationRepo;
    private final CurrentUser currentUser;

    public AdminController(SellerService sellers, OrderService orders, ReviewService reviews, MenuService menus,
                           ReportService reports, OrderRepository orderRepo, UserRepository users,
                           AuditLogRepository auditLogs, NotificationRepository notificationRepo, CurrentUser currentUser) {
        this.sellers = sellers;
        this.orders = orders;
        this.reviews = reviews;
        this.menus = menus;
        this.reports = reports;
        this.orderRepo = orderRepo;
        this.users = users;
        this.auditLogs = auditLogs;
        this.notificationRepo = notificationRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("stats", reports.platform());
        model.addAttribute("pending", sellers.pendingVerification());
        model.addAttribute("problems", orderRepo.findByStatusOrderByPlacedAtAsc(OrderStatus.DELIVERY_PROBLEM));
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/dashboard";
    }

    @GetMapping("/sellers")
    @Transactional(readOnly = true)
    public String sellers(Model model) {
        model.addAttribute("sellers", sellers.all());
        return "admin/sellers";
    }

    @PostMapping("/sellers/{id}/{action}")
    public String sellerAction(@PathVariable Long id, @PathVariable String action,
                               @RequestParam(required = false) String reason, RedirectAttributes flash) {
        String actor = currentUser.label();
        switch (action) {
            case "verify" -> sellers.verify(id, actor);
            case "reject" -> sellers.reject(id, reason, actor);
            case "suspend" -> sellers.suspend(id, reason, actor);
            case "reinstate" -> sellers.reinstate(id, actor);
            default -> throw new BusinessException("Unknown action.");
        }
        flash.addFlashAttribute("ok", "Seller updated.");
        return "redirect:/admin/sellers";
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String orders(Model model) {
        model.addAttribute("orders", orderRepo.findTop50ByOrderByPlacedAtDesc());
        return "admin/orders";
    }

    @GetMapping("/orders/{code}")
    @Transactional(readOnly = true)
    public String order(@PathVariable String code, Model model) {
        model.addAttribute("order", orders.byCode(code));
        return "admin/order";
    }

    @PostMapping("/orders/{code}/resolve")
    public String resolve(@PathVariable String code, @RequestParam OrderStatus outcome,
                          @RequestParam(required = false) String note, RedirectAttributes flash) {
        orders.adminResolve(code, outcome, note, currentUser.label());
        flash.addFlashAttribute("ok", "Order resolved.");
        return "redirect:/admin/orders/" + code;
    }

    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public String reviews(Model model) {
        model.addAttribute("reviews", reviews.all());
        return "admin/reviews";
    }

    @PostMapping("/reviews/{id}")
    public String moderate(@PathVariable Long id, @RequestParam boolean hidden, RedirectAttributes flash) {
        reviews.setHidden(id, hidden, currentUser.label());
        flash.addFlashAttribute("ok", hidden ? "Review hidden." : "Review visible again.");
        return "redirect:/admin/reviews";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", menus.allCategories());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name, @RequestParam(required = false) String icon, RedirectAttributes flash) {
        menus.addCategory(name, icon, currentUser.label());
        flash.addFlashAttribute("ok", "Category added.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategory(@PathVariable Long id) {
        menus.toggleCategory(id, currentUser.label());
        return "redirect:/admin/categories";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", users.findAllByOrderByCreatedAtDesc());
        return "admin/users";
    }

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("logs", auditLogs.findTop100ByOrderByAtDesc());
        model.addAttribute("notifications", notificationRepo.findTop100ByOrderByCreatedAtDesc());
        return "admin/audit";
    }
}
