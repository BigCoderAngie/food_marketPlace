package na.eat.marketplace.domain;

/**
 * Payment state, kept separate from the order state (FR-PY-01).
 * An order is only PAID once the seller confirms the money arrived (FR-PY-05).
 */
public enum PaymentStatus {
    UNPAID("Not paid yet"),
    AWAITING_CONFIRMATION("Customer says sent, seller to confirm"),
    PAID("Paid"),
    REFUND_DUE("Refund due"),
    REFUNDED("Refunded");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
