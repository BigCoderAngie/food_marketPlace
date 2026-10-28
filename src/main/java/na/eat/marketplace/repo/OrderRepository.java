package na.eat.marketplace.repo;

import na.eat.marketplace.domain.CustomerOrder;
import na.eat.marketplace.domain.OrderStatus;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByCode(String code);

    boolean existsByCode(String code);

    List<CustomerOrder> findByCustomerOrderByPlacedAtDesc(User customer);

    List<CustomerOrder> findBySellerAndStatusInOrderByPlacedAtAsc(Seller seller, Collection<OrderStatus> statuses);

    List<CustomerOrder> findBySellerOrderByPlacedAtDesc(Seller seller);

    List<CustomerOrder> findBySellerAndPlacedAtAfterOrderByPlacedAtDesc(Seller seller, Instant after);

    List<CustomerOrder> findByStatusAndAcceptByBefore(OrderStatus status, Instant now);

    List<CustomerOrder> findTop50ByOrderByPlacedAtDesc();

    List<CustomerOrder> findByStatusOrderByPlacedAtAsc(OrderStatus status);

    long countByStatus(OrderStatus status);

    long countByPlacedAtAfter(Instant after);

    @Query("select o.status, count(o) from CustomerOrder o group by o.status")
    List<Object[]> countByStatusGrouped();

    @Query("select count(o) from CustomerOrder o where o.seller = :seller and o.status = na.eat.marketplace.domain.OrderStatus.COMPLETED")
    int countCompleted(@Param("seller") Seller seller);
}
