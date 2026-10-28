package na.eat.marketplace.web.seller;

import na.eat.marketplace.domain.CustomerOrder;
import na.eat.marketplace.domain.OrderStatus;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Seller website (SRS 7.2): dashboard, orders, reviews and reports. */
@Controller
@RequestMapping("/seller")
public class SellerDashboardController {

    private final SellerSupport support;
    private final OrderService orders;
    private final ReportService reports;
    private final ReviewService reviews;
    private final MenuService menus;

    public SellerDashboardController(SellerSupport support, OrderService orders, ReportService reports,
                                     ReviewService reviews, MenuService menus) {
        this.support = support;
        this.orders = orders;
        this.reports = reports;
        this.reviews = reviews;
        this.menus = menus;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        Seller s = support.seller();
        List<CustomerOrder> active = orders.activeForSeller(s);
        model.addAttribute("seller", s);
        model.addAttribute("incoming", active.stream().filter(o -> o.getStatus() == OrderStatus.ORDER_PLACED).toList());
        model.addAttribute("inProgress", active.stream().filter(o -> o.getStatus() != OrderStatus.ORDER_PLACED).toList());
        model.addAttribute("report", reports.forSeller(s, 1));
        model.addAttribute("menuCount", menus.menu(s).size());
        model.addAttribute("today", menus.today());
        model.addAttribute("lowStock", menus.menu(s).stream()
                .filter(f -> f.portionsLeft(menus.today()) != null && f.portionsLeft(menus.today()) <= 3).toList());
        return "seller/dashboard";
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String orders(@RequestParam(defaultValue = "active") String tab, Model model) {
        Seller s = support.seller();
        model.addAttribute("seller", s);
        model.addAttribute("tab", tab);
        model.addAttribute("orders", "active".equals(tab) ? orders.activeForSeller(s)
                : orders.allForSeller(s).stream().filter(o -> o.getStatus().isFinal()).toList());
        return "seller/orders";
    }

    @GetMapping("/orders/{code}")
    @Transactional(readOnly = true)
    public String order(@PathVariable String code, Model model) {
        Seller s = support.seller();
        CustomerOrder o = orders.forSeller(s, code);
        model.addAttribute("seller", s);
        model.addAttribute("order", o);
        model.addAttribute("actions", SellerAction.forOrder(o));
        model.addAttribute("review", reviews.forOrder(o));
        return "seller/order";
    }

    @PostMapping("/orders/{code}/action")
    public String act(@PathVariable String code, @RequestParam("do") String action,
                      @RequestParam(required = false) String reason, RedirectAttributes flash) {
        Seller s = support.seller();
        String done = switch (action) {
            case "accept" -> { orders.accept(s, code); yield "Order accepted. The customer has been told."; }
            case "reject" -> { orders.reject(s, code, reason); yield "Order rejected."; }
            case "prepare" -> { orders.startPreparing(s, code); yield "Marked as preparing."; }
            case "ready" -> { orders.markReady(s, code); yield "Marked ready. The customer has been told."; }
            case "handover" -> { orders.handOver(s, code); yield "Handover recorded."; }
            case "delivered" -> { orders.markDelivered(s, code); yield "Marked delivered."; }
            case "problem" -> { orders.reportDeliveryProblem(s, code, reason); yield "Delivery problem reported to the customer and admin."; }
            case "complete" -> { orders.completeBySeller(s, code); yield "Order completed."; }
            case "cancel" -> { orders.cancelBySeller(s, code, reason); yield "Order cancelled."; }
            case "paid" -> { orders.confirmPayment(s, code); yield "Payment confirmed."; }
            case "refunded" -> { orders.markRefunded(s, code); yield "Refund recorded."; }
            default -> throw new BusinessException("Unknown action.");
        };
        flash.addFlashAttribute("ok", done);
        return "redirect:/seller/orders/" + code;
    }

    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public String reviews(Model model) {
        Seller s = support.seller();
        model.addAttribute("seller", s);
        model.addAttribute("reviews", reviews.allForSeller(s));
        return "seller/reviews";
    }

    @GetMapping("/reports")
    @Transactional(readOnly = true)
    public String reports(@RequestParam(defaultValue = "30") int days, Model model) {
        Seller s = support.seller();
        int d = Math.max(1, Math.min(days, 365));
        model.addAttribute("seller", s);
        model.addAttribute("report", reports.forSeller(s, d));
        return "seller/reports";
    }
}
