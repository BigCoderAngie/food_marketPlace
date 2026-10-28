package na.eat.marketplace.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lives in the HTTP session, so guests can fill a cart before logging in. One seller per cart (SRS 8.2). */
@Component
@SessionScope
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sellerId;
    private final Map<Long, Integer> quantities = new LinkedHashMap<>();

    public Long getSellerId() {
        return sellerId;
    }

    public Map<Long, Integer> getQuantities() {
        return quantities;
    }

    public boolean isEmpty() {
        return quantities.isEmpty();
    }

    public int getCount() {
        return quantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void put(Long sellerId, Long foodId, int quantity) {
        if (this.sellerId != null && !this.sellerId.equals(sellerId)) clear();
        this.sellerId = sellerId;
        if (quantity <= 0) quantities.remove(foodId);
        else quantities.put(foodId, quantity);
        if (quantities.isEmpty()) this.sellerId = null;
    }

    public void clear() {
        quantities.clear();
        sellerId = null;
    }
}
