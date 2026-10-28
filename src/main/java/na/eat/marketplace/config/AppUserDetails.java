package na.eat.marketplace.config;

import na.eat.marketplace.domain.Role;
import na.eat.marketplace.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user as Spring Security sees them. Holds the id so controllers can load the entity. */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String name;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getEmail() != null ? user.getEmail() : user.getPhone();
        this.name = user.getName();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.getStatus().canLogIn();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
