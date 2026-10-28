package na.eat.marketplace.web;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;

/** Public customer website (SRS 7.1). Browsing needs no account (FR-AC-06). */
@Controller
@Transactional(readOnly = true)
public class ShopController {

    private final SearchService search;
    private final MenuService menus;
    private final SellerService sellers;
    private final ReviewService reviews;
    private final FoodItemRepository foods;
    private final CartService carts;

    public ShopController(SearchService search, MenuService menus, SellerService sellers, ReviewService reviews,
                          FoodItemRepository foods, CartService carts) {
        this.search = search;
        this.menus = menus;
        this.sellers = sellers;
        this.reviews = reviews;
        this.foods = foods;
        this.carts = carts;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", menus.activeCategories());
        model.addAttribute("sellers", search.publicSellers().stream().limit(6).toList());
        model.addAttribute("foods", search.search(null, null, null, null, null, null).stream()
                .filter(f -> f.isOrderable(menus.today())).limit(9).toList());
        model.addAttribute("suburbs", search.suburbs());
        model.addAttribute("today", menus.today());
        return "shop/home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String category,
                         @RequestParam(required = false) String suburb,
                         @RequestParam(required = false) SellerType type,
                         @RequestParam(required = false) BigDecimal maxPrice,
                         @RequestParam(required = false) FulfilmentMethod fulfilment,
                         @RequestParam(defaultValue = "false") boolean availableNow,
                         Model model) {
        List<FoodItem> results = search.search(q, category, suburb, type, maxPrice, fulfilment);
        if (availableNow) results = results.stream().filter(f -> f.isOrderable(menus.today())).toList();
        model.addAttribute("results", results);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("suburb", suburb);
        model.addAttribute("type", type);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("fulfilment", fulfilment);
        model.addAttribute("availableNow", availableNow);
        model.addAttribute("categories", menus.activeCategories());
        model.addAttribute("suburbs", search.suburbs());
        model.addAttribute("sellerTypes", SellerType.values());
        model.addAttribute("today", menus.today());
        return "shop/search";
    }

    @GetMapping("/sellers")
    public String sellers(Model model) {
        model.addAttribute("sellers", search.publicSellers());
        return "shop/sellers";
    }

    @GetMapping("/food/{id}")
    public String food(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean conflict, Model model) {
        FoodItem f = foods.findLive(id).orElseThrow(() -> new NotFoundException("Food not found"));
        if (!f.getSeller().isPublic() || f.getStatus() == FoodStatus.UNAVAILABLE) throw new NotFoundException("Food not found");
        model.addAttribute("food", f);
        model.addAttribute("seller", f.getSeller());
        model.addAttribute("today", menus.today());
        model.addAttribute("more", menus.menu(f.getSeller()).stream()
                .filter(x -> !x.getId().equals(id) && x.getStatus() != FoodStatus.UNAVAILABLE).limit(4).toList());
        if (conflict) {
            CartView cart = carts.view();
            model.addAttribute("conflictSeller", cart.seller());
        }
        return "shop/food";
    }

    @GetMapping("/s/{slug}")
    public String seller(@PathVariable String slug, Model model) {
        Seller s = sellers.publicBySlug(slug);
        List<FoodItem> menu = menus.menu(s).stream().filter(f -> f.getStatus() != FoodStatus.UNAVAILABLE).toList();
        Map<String, List<FoodItem>> grouped = new LinkedHashMap<>();
        for (FoodItem f : menu) {
            grouped.computeIfAbsent(f.getCategory() == null ? "Other" : f.getCategory().getName(), k -> new ArrayList<>()).add(f);
        }
        model.addAttribute("seller", s);
        model.addAttribute("menu", grouped);
        model.addAttribute("reviews", reviews.publicForSeller(s).stream().limit(10).toList());
        model.addAttribute("today", menus.today());
        return "shop/seller";
    }
}
