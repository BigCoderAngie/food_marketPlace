package na.eat.marketplace.service;

import na.eat.marketplace.config.AppUserDetails;
import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.SellerRepository;
import na.eat.marketplace.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Registration and login by email or phone (FR-AC-01, FR-AC-02). */
@Service
public class AccountService implements UserDetailsService {

    private final UserRepository users;
    private final SellerRepository sellers;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AccountService(UserRepository users, SellerRepository sellers, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.sellers = sellers;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return findByLogin(username)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + username));
    }

    public Optional<User> findByLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        String value = login.trim();
        if (value.contains("@")) return users.findByEmailIgnoreCase(value);
        return users.findByPhone(Phones.normalise(value));
    }

    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public User registerCustomer(String name, String email, String phone, String password) {
        User user = newUser(name, email, phone, password, Role.CUSTOMER);
        user.setStatus(AccountStatus.ACTIVE);
        return users.save(user);
    }

    /**
     * Seller accounts start UNDER_REVIEW with a PENDING_REVIEW storefront: they can build their
     * menu straight away, but nothing is public until an admin verifies them (SRS 2.4).
     */
    @Transactional
    public Seller registerSeller(String ownerName, String email, String phone, String password,
                                 String businessName, SellerType type, String suburb, String description) {
        if (businessName == null || businessName.isBlank()) throw new BusinessException("Enter your business name.");
        if (suburb == null || suburb.isBlank()) throw new BusinessException("Enter the suburb you cook or sell from.");
        User owner = newUser(ownerName, email, phone, password, Role.SELLER);
        owner.setStatus(AccountStatus.UNDER_REVIEW);
        users.save(owner);

        Seller seller = new Seller();
        seller.setOwner(owner);
        seller.setName(businessName.trim());
        seller.setSlug(uniqueSlug(businessName));
        seller.setType(type == null ? SellerType.HOME_COOK : type);
        seller.setSuburb(suburb.trim());
        seller.setDescription(description);
        seller.setPhone(owner.getPhone());
        seller.setWhatsapp(owner.getPhone());
        sellers.save(seller);
        audit.log(owner.getContact(), "SELLER_REGISTERED", "Seller", seller.getId(), seller.getName());
        return seller;
    }

    private User newUser(String name, String email, String phone, String password, Role role) {
        if (name == null || name.isBlank()) throw new BusinessException("Enter your name.");
        String cleanEmail = email == null || email.isBlank() ? null : email.trim().toLowerCase();
        String cleanPhone = phone == null || phone.isBlank() ? null : Phones.normalise(phone);
        if (cleanEmail == null && cleanPhone == null) throw new BusinessException("Enter a phone number or an email address.");
        if (cleanEmail != null && !cleanEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new BusinessException("That email address doesn't look right.");
        if (cleanPhone != null && !Phones.isValid(cleanPhone)) throw new BusinessException("Enter a Namibian phone number, e.g. 081 234 5678.");
        if (cleanEmail != null && users.existsByEmailIgnoreCase(cleanEmail)) throw new BusinessException("An account with that email already exists.");
        if (cleanPhone != null && users.existsByPhone(cleanPhone)) throw new BusinessException("An account with that phone number already exists.");
        if (password == null || password.length() < 8) throw new BusinessException("Use a password of at least 8 characters.");

        User user = new User();
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        user.setPhone(cleanPhone);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        return user;
    }

    private String uniqueSlug(String name) {
        String base = Slugs.of(name);
        String slug = base;
        for (int i = 2; sellers.existsBySlug(slug); i++) slug = base + "-" + i;
        return slug;
    }
}
