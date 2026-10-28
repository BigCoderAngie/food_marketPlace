package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.CategoryRepository;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.repo.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final FoodItemRepository foods;
    private final SellerRepository sellers;
    private final CategoryRepository categories;

    public SearchService(FoodItemRepository foods, SellerRepository sellers, CategoryRepository categories) {
        this.foods = foods;
        this.sellers = sellers;
        this.categories = categories;
    }

    public List<FoodItem> search(String q, String categorySlug, String suburb, SellerType type,
                                 BigDecimal maxPrice, FulfilmentMethod fulfilment) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        Category category = categorySlug == null || categorySlug.isBlank() ? null : categories.findBySlug(categorySlug).orElse(null);
        String sub = suburb == null || suburb.isBlank() ? null : suburb.trim().toLowerCase(Locale.ROOT);
        return foods.search(like, category, sub, type, maxPrice,
                fulfilment == FulfilmentMethod.DELIVERY, fulfilment == FulfilmentMethod.PICKUP);
    }

    public List<Seller> publicSellers() {
        return sellers.findPublic();
    }

    public List<String> suburbs() {
        return sellers.findPublicSuburbs();
    }

    /**
     * Alternatives when an order is rejected or expires (SRS change #3): verified sellers that sell
     * the same kinds of food, preferring the same suburb, then any other open seller.
     */
    public List<Seller> similarSellers(CustomerOrder order, int limit) {
        Seller original = order.getSeller();
        Set<Category> cats = order.getItems().stream()
                .map(i -> i.getFoodItem().getCategory()).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Seller> candidates = new ArrayList<>(cats.isEmpty() ? List.of() : foods.sellersWithCategories(cats, original));
        for (Seller s : sellers.findPublic()) {
            if (!s.getId().equals(original.getId()) && candidates.stream().noneMatch(c -> c.getId().equals(s.getId()))) {
                candidates.add(s);
            }
        }
        candidates.sort(Comparator
                .comparing((Seller s) -> !s.getSuburb().equalsIgnoreCase(original.getSuburb()))
                .thenComparing(s -> s.getRatingAverage() == null ? BigDecimal.ZERO : s.getRatingAverage(), Comparator.reverseOrder()));
        return candidates.stream().filter(Seller::isOpenForOrders).limit(limit).toList();
    }
}
