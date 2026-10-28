package na.eat.marketplace.service;

import na.eat.marketplace.domain.FoodItem;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.repo.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** FR-CT-01..04. */
@Service
public class CartService {

    public static final int MAX_PER_ITEM = 50;

    private final Cart cart;
    private final FoodItemRepository foods;
    private final SellerRepository sellers;
    private final Clock clock;

    public CartService(Cart cart, FoodItemRepository foods, SellerRepository sellers, Clock clock) {
        this.cart = cart;
        this.foods = foods;
        this.sellers = sellers;
        this.clock = clock;
    }

    public Cart cart() {
        return cart;
    }

    /**
     * Adds an item. Returns false without changing anything when the cart holds another seller's
     * food and the customer hasn't confirmed starting a new cart.
     */
    @Transactional(readOnly = true)
    public boolean add(Long foodId, int quantity, boolean replaceOtherSeller) {
        FoodItem f = foods.findLive(foodId).orElseThrow(() -> new NotFoundException("Food item not found"));
        if (!f.getSeller().isOpenForOrders()) throw new BusinessException(f.getSeller().getName() + " isn't taking orders right now.");
        if (!f.isOrderable(LocalDate.now(clock))) throw new BusinessException(f.getName() + " is not available right now.");
        if (cart.getSellerId() != null && !cart.getSellerId().equals(f.getSeller().getId()) && !replaceOtherSeller) {
            return false;
        }
        int current = f.getSeller().getId().equals(cart.getSellerId()) ? cart.getQuantities().getOrDefault(foodId, 0) : 0;
        int wanted = Math.min(MAX_PER_ITEM, current + Math.max(1, quantity));
        Integer left = f.portionsLeft(LocalDate.now(clock));
        if (left != null && wanted > left) {
            throw new BusinessException("Only " + left + " portion" + (left == 1 ? "" : "s") + " of " + f.getName() + " left today.");
        }
        cart.put(f.getSeller().getId(), foodId, wanted);
        return true;
    }

    public void update(Long foodId, int quantity) {
        if (cart.getSellerId() == null) return;
        cart.put(cart.getSellerId(), foodId, Math.min(MAX_PER_ITEM, quantity));
    }

    public void clear() {
        cart.clear();
    }

    /** Recalculates totals from current prices and availability each time (FR-CT-03, FR-CT-04). */
    @Transactional(readOnly = true)
    public CartView view() {
        if (cart.isEmpty()) return new CartView(null, List.of(), BigDecimal.ZERO, 0, List.of());
        Seller seller = sellers.findById(cart.getSellerId()).orElse(null);
        if (seller == null) {
            cart.clear();
            return new CartView(null, List.of(), BigDecimal.ZERO, 0, List.of());
        }
        LocalDate today = LocalDate.now(clock);
        List<CartView.Line> lines = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int maxPrep = 0;
        for (Map.Entry<Long, Integer> e : List.copyOf(cart.getQuantities().entrySet())) {
            FoodItem f = foods.findById(e.getKey()).orElse(null);
            if (f == null || f.isDeleted()) {
                cart.getQuantities().remove(e.getKey());
                continue;
            }
            Integer left = f.portionsLeft(today);
            boolean ok = f.isOrderable(today) && (left == null || left >= e.getValue());
            if (!f.isOrderable(today)) problems.add(f.getName() + " is no longer available.");
            else if (!ok) problems.add("Only " + left + " portion" + (left == 1 ? "" : "s") + " of " + f.getName() + " left.");
            BigDecimal line = f.getPrice().multiply(BigDecimal.valueOf(e.getValue()));
            subtotal = subtotal.add(line);
            maxPrep = Math.max(maxPrep, f.getPrepMinutes());
            lines.add(new CartView.Line(f, e.getValue(), line, left, ok));
        }
        if (!seller.isOpenForOrders()) problems.add(seller.getName() + " isn't taking orders right now.");
        if (cart.getQuantities().isEmpty()) cart.clear();
        return new CartView(seller, lines, subtotal, maxPrep, problems);
    }
}
