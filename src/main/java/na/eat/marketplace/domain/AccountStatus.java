package na.eat.marketplace.domain;

/** FR-AC-05: minimum account states. */
public enum AccountStatus {
    NEW, ACTIVE, UNDER_REVIEW, SUSPENDED, CLOSED;

    public boolean canLogIn() {
        return this == NEW || this == ACTIVE || this == UNDER_REVIEW;
    }
}
