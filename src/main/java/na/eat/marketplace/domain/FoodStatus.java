package na.eat.marketplace.domain;

/** FR-FD-03. */
public enum FoodStatus {
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    SOLD_OUT("Sold out");

    private final String label;

    FoodStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
