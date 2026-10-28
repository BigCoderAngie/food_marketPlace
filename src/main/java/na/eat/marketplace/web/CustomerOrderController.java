package na.eat.marketplace.web;

import na.eat.marketplace.domain.CustomerOrder;
import na.eat.marketplace.domain.OrderStatus;
import na.eat.marketplace.domain.User;
import na.eat.marketplace.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Order confirmation, tracking and history (FR-OR-05). */
@Controller
@RequestMapping("/orders")
public class CustomerOrderController {

    private final OrderService orders;
    private final ReviewService reviews;
    private final SearchService search;
    private final CurrentUser currentUser;

    public CustomerOrderController(OrderService orders, ReviewService reviews, SearchService search, CurrentUser currentUser) {
        this.orders = orders;
        this.reviews = reviews;
        this.search = search;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(Model model) {
        var all = orders.customerOrders(currentUser.get());
        model.addAttribute("active", all.stream().filter(o -> o.getStatus().isActive()).toList());
        model.addAttribute("past", all.stream().filter(o -> o.getStatus().isFinal()).toList());
        return "orders/list";
    }

    @GetMapping("/{code}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable String code, Model model) {
        CustomerOrder o = orders.forCustomer(currentUser.get(), code);
        model.addAttribute("order", o);
        model.addAttribute("canReview", reviews.canReview(o));
        model.addAttribute("review", reviews.forOrder(o));
        if (o.getStatus() == OrderStatus.REJECTED || o.getStatus() == OrderStatus.EXPIRED) {
            model.addAttribute("alternatives", search.similarSellers(o, 3));
        }
        return "orders/detail";
    }

    @PostMapping("/{code}/cancel")
    public String cancel(@PathVariable String code, @RequestParam(required = false) String reason, RedirectAttributes flash) {
        orders.cancelByCustomer(currentUser.get(), code, reason);
        flash.addFlashAttribute("ok", "Your order was cancelled.");
        return "redirect:/orders/" + code;
    }

    @PostMapping("/{code}/paid")
    public String paid(@PathVariable String code, @RequestParam(required = false) String reference, RedirectAttributes flash) {
        orders.markPaymentSent(currentUser.get(), code, reference);
        flash.addFlashAttribute("ok", "Thanks. The seller will confirm when the money arrives.");
        return "redirect:/orders/" + code;
    }

    @PostMapping("/{code}/received")
    public String received(@PathVariable String code, RedirectAttributes flash) {
        orders.confirmReceived(currentUser.get(), code);
        flash.addFlashAttribute("ok", "Enjoy your food! You can now leave a review.");
        return "redirect:/orders/" + code;
    }

    @PostMapping("/{code}/review")
    public String review(@PathVariable String code, @RequestParam int rating,
                         @RequestParam(required = false) String comment, RedirectAttributes flash) {
        User me = currentUser.get();
        reviews.submit(me, code, rating, comment);
        flash.addFlashAttribute("ok", "Thanks for your review.");
        return "redirect:/orders/" + code;
    }
}
