package na.eat.marketplace.service;

import na.eat.marketplace.domain.FoodItem;
import na.eat.marketplace.domain.Seller;

import java.math.BigDecimal;
import java.util.List;

public record CartView(Seller seller, List<Line> lines, BigDecimal subtotal, int maxPrepMinutes, List<String> problems) {

    public record Line(FoodItem food, int quantity, BigDecimal lineTotal, Integer portionsLeft, boolean orderable) {
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean isOk() {
        return !lines.isEmpty() && problems.isEmpty();
    }
}
