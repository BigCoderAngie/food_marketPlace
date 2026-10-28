package na.eat.marketplace.web.seller;

import jakarta.validation.Valid;
import na.eat.marketplace.domain.FoodStatus;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.service.BusinessException;
import na.eat.marketplace.service.FoodForm;
import na.eat.marketplace.service.MenuService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Menu and availability (SRS 7.2, FR-FD-01..07). */
@Controller
@RequestMapping("/seller/menu")
public class SellerMenuController {

    private final SellerSupport support;
    private final MenuService menus;

    public SellerMenuController(SellerSupport support, MenuService menus) {
        this.support = support;
        this.menus = menus;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String list(Model model) {
        Seller s = support.seller();
        model.addAttribute("seller", s);
        model.addAttribute("items", menus.menu(s));
        model.addAttribute("today", menus.today());
        model.addAttribute("statuses", FoodStatus.values());
        return "seller/menu";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(new FoodForm(), null, model);
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String edit(@PathVariable Long id, Model model) {
        var item = menus.own(support.seller(), id);
        model.addAttribute("photoPath", item.getPhotoPath());
        return form(FoodForm.from(item), id, model);
    }

    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Long id, @Valid @ModelAttribute("form") FoodForm form,
                       BindingResult errors, @RequestParam(required = false) MultipartFile photo,
                       Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) return form(form, id, model);
        try {
            menus.save(support.seller(), id, form, photo, support.actor());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return form(form, id, model);
        }
        flash.addFlashAttribute("ok", id == null ? "Item added to your menu." : "Item saved.");
        return "redirect:/seller/menu";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @RequestParam FoodStatus status, RedirectAttributes flash) {
        menus.setStatus(support.seller(), id, status, support.actor());
        flash.addFlashAttribute("ok", "Availability updated.");
        return "redirect:/seller/menu";
    }

    @PostMapping("/{id}/portions")
    public String portions(@PathVariable Long id, @RequestParam(required = false) Integer portions, RedirectAttributes flash) {
        menus.setTodaysPortions(support.seller(), id, portions, support.actor());
        flash.addFlashAttribute("ok", portions == null ? "Daily limit removed." : "Today's portions set to " + portions + ".");
        return "redirect:/seller/menu";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        menus.delete(support.seller(), id, support.actor());
        flash.addFlashAttribute("ok", "Item removed from your menu.");
        return "redirect:/seller/menu";
    }

    private String form(FoodForm form, Long id, Model model) {
        model.addAttribute("seller", support.seller());
        model.addAttribute("form", form);
        model.addAttribute("id", id);
        model.addAttribute("categories", menus.activeCategories());
        model.addAttribute("statuses", FoodStatus.values());
        return "seller/menu-form";
    }
}
