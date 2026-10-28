package na.eat.marketplace.domain;

public enum FulfilmentMethod {
    PICKUP("Pickup"),
    DELIVERY("Delivery");

    private final String label;

    FulfilmentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
