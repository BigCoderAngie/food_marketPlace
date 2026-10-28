package na.eat.marketplace.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Role-aware access (SRS 7.4). Public pages are open so customers can browse
 * without an account (FR-AC-06); checkout and management areas need a login.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/uploads/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/register/**", "/login").permitAll()
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/checkout/**", "/orders/**", "/notifications/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .successHandler(new RoleHomeSuccessHandler())
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?loggedOut"));
        return http.build();
    }

    /** Back to the page that needed a login, otherwise to the user's own home area. */
    static class RoleHomeSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
        @Override
        protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) {
            if (authentication.getPrincipal() instanceof AppUserDetails user) {
                return switch (user.getRole()) {
                    case ADMIN -> "/admin";
                    case SELLER -> "/seller";
                    case CUSTOMER -> "/";
                };
            }
            return "/";
        }
    }
}
