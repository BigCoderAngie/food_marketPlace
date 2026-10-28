package na.eat.marketplace.domain;

/** Launch payment methods (FR-PY-02). Card payments are deferred until a gateway is chosen. */
public enum PaymentMethod {
    CASH("Cash on pickup/delivery"),
    EWALLET("eWallet / Blue Wallet / EasyWallet"),
    EFT("Bank EFT");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTransfer() {
        return this != CASH;
    }
}
