package na.eat.marketplace.repo;

import na.eat.marketplace.domain.DeliveryZone;
import na.eat.marketplace.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {

    Optional<DeliveryZone> findBySellerAndSuburbIgnoreCase(Seller seller, String suburb);
}
