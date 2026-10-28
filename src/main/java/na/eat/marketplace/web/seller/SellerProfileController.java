package na.eat.marketplace.web.seller;

import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.SellerType;
import na.eat.marketplace.service.BusinessException;
import na.eat.marketplace.service.SellerProfileForm;
import na.eat.marketplace.service.SellerService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Public profile and operating settings, delivery suburbs and pause (FR-SL-01, FR-SL-05, FR-DL-03). */
@Controller
@RequestMapping("/seller/profile")
public class SellerProfileController {

    private final SellerSupport support;
    private final SellerService sellers;

    public SellerProfileController(SellerSupport support, SellerService sellers) {
        this.support = support;
        this.sellers = sellers;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String view(Model model) {
        Seller s = support.seller();
        return render(s, SellerProfileForm.from(s), model);
    }

    @PostMapping
    public String save(@ModelAttribute("form") SellerProfileForm form, Model model, RedirectAttributes flash) {
        Seller s = support.seller();
        try {
            sellers.updateProfile(s, form, support.actor());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return render(s, form, model);
        }
        flash.addFlashAttribute("ok", "Profile saved.");
        return "redirect:/seller/profile";
    }

    @PostMapping("/zones")
    public String addZone(@RequestParam String suburb, @RequestParam(required = false) BigDecimal fee, RedirectAttributes flash) {
        sellers.addZone(support.seller(), suburb, fee, support.actor());
        flash.addFlashAttribute("ok", "Delivery suburb added.");
        return "redirect:/seller/profile#delivery";
    }

    @PostMapping("/zones/{id}/delete")
    public String removeZone(@PathVariable Long id, RedirectAttributes flash) {
        sellers.removeZone(support.seller(), id, support.actor());
        flash.addFlashAttribute("ok", "Delivery suburb removed.");
        return "redirect:/seller/profile#delivery";
    }

    @PostMapping("/pause")
    public String pause(@RequestParam boolean paused, RedirectAttributes flash) {
        sellers.setPaused(support.seller(), paused, support.actor());
        flash.addFlashAttribute("ok", paused ? "Your storefront is paused. Customers can't order until you resume."
                : "Your storefront is open again.");
        return "redirect:/seller";
    }

    private String render(Seller s, SellerProfileForm form, Model model) {
        model.addAttribute("seller", s);
        model.addAttribute("form", form);
        model.addAttribute("sellerTypes", SellerType.values());
        return "seller/profile";
    }
}
