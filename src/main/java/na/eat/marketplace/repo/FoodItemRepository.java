package na.eat.marketplace.repo;

import jakarta.persistence.LockModeType;
import na.eat.marketplace.domain.Category;
import na.eat.marketplace.domain.FoodItem;
import na.eat.marketplace.domain.Seller;
import na.eat.marketplace.domain.SellerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findBySellerAndDeletedFalseOrderByCategory_SortOrderAscNameAsc(Seller seller);

    /** Locks rows so two customers can't both take the last portion (SRS 5.1). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FoodItem f where f.id in :ids order by f.id")
    List<FoodItem> lockByIds(@Param("ids") Collection<Long> ids);

    @Query("select f from FoodItem f join fetch f.seller s left join fetch f.category where f.id = :id and f.deleted = false")
    Optional<FoodItem> findLive(@Param("id") Long id);

    /**
     * Public search (FR-SR-01..03): only listings from verified, unpaused sellers, never deleted
     * or unavailable items. Sold-out items still show so customers see them, but can't order them.
     */
    @Query("""
            select f from FoodItem f join fetch f.seller s left join fetch f.category c
            where f.deleted = false
              and f.status <> na.eat.marketplace.domain.FoodStatus.UNAVAILABLE
              and s.status = na.eat.marketplace.domain.SellerStatus.VERIFIED and s.paused = false
              and (:q is null or lower(f.name) like :q or lower(f.description) like :q
                   or lower(s.name) like :q or lower(c.name) like :q or lower(f.dietaryTags) like :q)
              and (:category is null or c = :category)
              and (:suburb is null or lower(s.suburb) = :suburb)
              and (:sellerType is null or s.type = :sellerType)
              and (:maxPrice is null or f.price <= :maxPrice)
              and (:delivery = false or s.offersDelivery = true)
              and (:pickup = false or s.offersPickup = true)
            order by s.ratingAverage desc nulls last, s.completedOrders desc, f.name
            """)
    List<FoodItem> search(@Param("q") String q,
                          @Param("category") Category category,
                          @Param("suburb") String suburb,
                          @Param("sellerType") SellerType sellerType,
                          @Param("maxPrice") BigDecimal maxPrice,
                          @Param("delivery") boolean delivery,
                          @Param("pickup") boolean pickup);

    @Query("""
            select distinct f.seller from FoodItem f
            where f.deleted = false and f.category in :categories
              and f.seller <> :exclude
              and f.seller.status = na.eat.marketplace.domain.SellerStatus.VERIFIED and f.seller.paused = false
            """)
    List<Seller> sellersWithCategories(@Param("categories") Collection<Category> categories,
                                       @Param("exclude") Seller exclude);
}
