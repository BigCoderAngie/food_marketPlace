package na.eat.marketplace.repo;

import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.SellerStatus;
import na.eat.marketplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findBySlug(String slug);

    Optional<Seller> findByOwner(User owner);

    boolean existsBySlug(String slug);

    List<Seller> findByStatusOrderByCreatedAtAsc(SellerStatus status);

    List<Seller> findAllByOrderByCreatedAtDesc();

    long countByStatus(SellerStatus status);

    @Query("select s from Seller s where s.status = na.eat.marketplace.domain.SellerStatus.VERIFIED and s.paused = false order by s.ratingAverage desc nulls last, s.completedOrders desc")
    List<Seller> findPublic();

    @Query("select distinct s.suburb from Seller s where s.status = na.eat.marketplace.domain.SellerStatus.VERIFIED order by s.suburb")
    List<String> findPublicSuburbs();
}
