package na.eat.marketplace.web;

import na.eat.marketplace.domain.CustomerOrder;
import na.eat.marketplace.domain.PaymentMethod;
import na.eat.marketplace.domain.Role;
import na.eat.marketplace.domain.User;
import na.eat.marketplace.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** FR-CT-05, FR-CT-06, FR-OR-02. */
@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService carts;
    private final OrderService orders;
    private final CurrentUser currentUser;

    public CheckoutController(CartService carts, OrderService orders, CurrentUser currentUser) {
        this.carts = carts;
        this.orders = orders;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String form(Model model) {
        CheckoutForm form = new CheckoutForm();
        currentUser.find().map(User::getPhone).ifPresent(form::setContactPhone);
        return render(form, model);
    }

    @PostMapping
    public String place(@ModelAttribute("form") CheckoutForm form, Model model) {
        User user = currentUser.get();
        try {
            CustomerOrder o = orders.place(user, carts.cart(), form);
            return "redirect:/orders/" + o.getCode() + "?placed";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return render(form, model);
        }
    }

    private String render(CheckoutForm form, Model model) {
        CartView cart = carts.view();
        if (cart.isEmpty()) return "redirect:/cart";
        model.addAttribute("cart", cart);
        model.addAttribute("form", form);
        model.addAttribute("slots", orders.pickupSlots(cart.maxPrepMinutes()));
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("isCustomer", currentUser.details().map(d -> d.getRole() == Role.CUSTOMER).orElse(false));
        if (form.getPickupTime() == null && cart.seller().isOffersPickup()) {
            form.setPickupTime(orders.earliestPickup(cart.maxPrepMinutes()));
        }
        if (!cart.seller().isOffersPickup()) form.setFulfilment(na.eat.marketplace.domain.FulfilmentMethod.DELIVERY);
        return "shop/checkout";
    }
}
