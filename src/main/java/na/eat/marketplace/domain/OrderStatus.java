package na.eat.marketplace.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/**
 * Authoritative order workflow (SRS section 5). Transitions are enforced server-side
 * through {@link #canMoveTo(OrderStatus)} (FR-OR-08).
 */
public enum OrderStatus {
    ORDER_PLACED("Waiting for seller"),
    ACCEPTED("Accepted"),
    PREPARING("Preparing"),
    READY("Ready"),
    OUT_FOR_DELIVERY("Out for delivery"),
    PICKED_UP("Collected"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled"),
    DELIVERY_PROBLEM("Delivery problem");

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = Map.ofEntries(
            entry(ORDER_PLACED, EnumSet.of(ACCEPTED, REJECTED, EXPIRED, CANCELLED)),
            entry(ACCEPTED, EnumSet.of(PREPARING, CANCELLED)),
            entry(PREPARING, EnumSet.of(READY, CANCELLED)),
            entry(READY, EnumSet.of(PICKED_UP, OUT_FOR_DELIVERY, CANCELLED)),
            entry(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, DELIVERY_PROBLEM)),
            entry(PICKED_UP, EnumSet.of(COMPLETED)),
            entry(DELIVERED, EnumSet.of(COMPLETED)),
            entry(DELIVERY_PROBLEM, EnumSet.of(DELIVERED, COMPLETED, CANCELLED)),
            entry(COMPLETED, EnumSet.noneOf(OrderStatus.class)),
            entry(REJECTED, EnumSet.noneOf(OrderStatus.class)),
            entry(EXPIRED, EnumSet.noneOf(OrderStatus.class)),
            entry(CANCELLED, EnumSet.noneOf(OrderStatus.class)));

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canMoveTo(OrderStatus target) {
        return NEXT.get(this).contains(target);
    }

    public Set<OrderStatus> next() {
        return NEXT.get(this);
    }

    public boolean isFinal() {
        return NEXT.get(this).isEmpty();
    }

    /** Orders that ended without the food changing hands. */
    public boolean isFailed() {
        return this == REJECTED || this == EXPIRED || this == CANCELLED;
    }

    public boolean isActive() {
        return !isFinal();
    }
}
