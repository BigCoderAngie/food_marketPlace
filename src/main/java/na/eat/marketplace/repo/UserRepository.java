package na.eat.marketplace.repo;

import na.eat.marketplace.domain.Role;
import na.eat.marketplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    long countByRole(Role role);

    List<User> findAllByOrderByCreatedAtDesc();
}
