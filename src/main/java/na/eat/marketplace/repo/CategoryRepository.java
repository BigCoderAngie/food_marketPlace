package na.eat.marketplace.repo;

import na.eat.marketplace.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
