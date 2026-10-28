package na.eat.marketplace.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import na.eat.marketplace.config.AppUserDetails;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.SellerType;
import na.eat.marketplace.domain.User;
import na.eat.marketplace.service.AccountService;
import na.eat.marketplace.service.BusinessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AccountService accounts;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
        SavedRequest saved = new HttpSessionRequestCache().getRequest(request, response);
        model.addAttribute("continuing", saved != null);
        model.addAttribute("continuingCheckout", saved != null && saved.getRedirectUrl().contains("/checkout"));
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerCustomer(@RequestParam String name, @RequestParam(required = false) String email,
                                   @RequestParam(required = false) String phone, @RequestParam String password,
                                   @RequestParam(required = false) String next,
                                   HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            User user = accounts.registerCustomer(name, email, phone, password);
            signIn(user, request, response);
            return "redirect:" + ("checkout".equals(next) ? "/checkout" : "/?welcome");
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("next", next);
            return "auth/register";
        }
    }

    @GetMapping("/register/seller")
    public String registerSellerForm(Model model) {
        model.addAttribute("sellerTypes", SellerType.values());
        return "auth/register-seller";
    }

    @PostMapping("/register/seller")
    public String registerSeller(@RequestParam String name, @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String phone, @RequestParam String password,
                                 @RequestParam String businessName, @RequestParam(required = false) SellerType type,
                                 @RequestParam String suburb, @RequestParam(required = false) String description,
                                 HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            Seller seller = accounts.registerSeller(name, email, phone, password, businessName, type, suburb, description);
            signIn(seller.getOwner(), request, response);
            return "redirect:/seller?welcome";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("sellerTypes", SellerType.values());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("businessName", businessName);
            model.addAttribute("type", type);
            model.addAttribute("suburb", suburb);
            model.addAttribute("description", description);
            return "auth/register-seller";
        }
    }

    /** Log the new account straight in, keeping the session (and cart) but with a fresh id. */
    private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        AppUserDetails details = new AppUserDetails(user);
        var auth = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        request.changeSessionId();
        contextRepository.saveContext(context, request, response);
    }
}
