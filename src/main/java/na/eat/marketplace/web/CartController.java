package na.eat.marketplace.web;

import na.eat.marketplace.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public String view(Model model) {
        model.addAttribute("cart", carts.view());
        return "shop/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam Long foodId, @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "false") boolean replace,
                      @RequestParam(required = false) String back, RedirectAttributes flash) {
        if (!carts.add(foodId, quantity, replace)) {
            return "redirect:/food/" + foodId + "?conflict=true";
        }
        flash.addFlashAttribute("ok", "Added to your cart.");
        return "redirect:" + (back != null && back.startsWith("/") && !back.startsWith("//") ? back : "/cart");
    }

    @PostMapping("/update")
    public String update(@RequestParam Long foodId, @RequestParam int quantity) {
        carts.update(foodId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear() {
        carts.clear();
        return "redirect:/cart";
    }
}
