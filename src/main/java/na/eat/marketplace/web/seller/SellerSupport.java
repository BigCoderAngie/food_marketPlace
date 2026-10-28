package na.eat.marketplace.web.seller;

import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.service.SellerService;
import na.eat.marketplace.web.CurrentUser;
import org.springframework.stereotype.Component;

@Component
public class SellerSupport {

    private final SellerService sellers;
    private final CurrentUser currentUser;

    public SellerSupport(SellerService sellers, CurrentUser currentUser) {
        this.sellers = sellers;
        this.currentUser = currentUser;
    }

    public Seller seller() {
        return sellers.forOwner(currentUser.get());
    }

    public String actor() {
        return currentUser.label();
    }
}
