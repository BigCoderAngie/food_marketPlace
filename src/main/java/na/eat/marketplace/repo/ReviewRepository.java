package na.eat.marketplace.repo;

import na.eat.marketplace.domain.CustomerOrder;
import na.eat.marketplace.domain.Review;
import na.eat.marketplace.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrder(CustomerOrder order);

    boolean existsByOrder(CustomerOrder order);

    List<Review> findBySellerAndHiddenFalseOrderByCreatedAtDesc(Seller seller);

    List<Review> findBySellerOrderByCreatedAtDesc(Seller seller);

    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("select avg(r.rating), count(r) from Review r where r.seller = :seller and r.hidden = false")
    List<Object[]> aggregate(@Param("seller") Seller seller);
}
