package na.eat.marketplace.domain;

/** FR-SL-02. */
public enum SellerType {
    RESTAURANT("Restaurant"),
    HOME_COOK("Home cook"),
    FOOD_TRUCK("Food truck / stall"),
    CATERER("Caterer"),
    BAKERY("Bakery");

    private final String label;

    SellerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
