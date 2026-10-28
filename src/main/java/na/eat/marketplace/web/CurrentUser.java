package na.eat.marketplace.web;

import na.eat.marketplace.config.AppUserDetails;
import na.eat.marketplace.domain.User;
import na.eat.marketplace.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUser {

    private final AccountService accounts;

    public CurrentUser(AccountService accounts) {
        this.accounts = accounts;
    }

    public Optional<AppUserDetails> details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails d) return Optional.of(d);
        return Optional.empty();
    }

    public Optional<User> find() {
        return details().map(d -> accounts.get(d.getId()));
    }

    /** For pages behind authentication, where a user is guaranteed. */
    public User get() {
        return find().orElseThrow(() -> new IllegalStateException("Not logged in"));
    }

    public String label() {
        return details().map(d -> d.getName() + " <" + d.getUsername() + ">").orElse("anonymous");
    }
}
