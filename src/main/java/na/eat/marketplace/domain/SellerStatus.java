package na.eat.marketplace.domain;

/** Verification state of a seller. Only VERIFIED sellers are public (FR-SL-03). */
public enum SellerStatus {
    PENDING_REVIEW("Awaiting verification"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    SUSPENDED("Suspended");

    private final String label;

    SellerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
