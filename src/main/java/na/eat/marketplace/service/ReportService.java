package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.OrderRepository;
import na.eat.marketplace.repo.SellerRepository;
import na.eat.marketplace.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Seller reports (FR-RP-01) and platform statistics (FR-AD-03). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orders;
    private final SellerRepository sellers;
    private final UserRepository users;
    private final Clock clock;

    public ReportService(OrderRepository orders, SellerRepository sellers, UserRepository users, Clock clock) {
        this.orders = orders;
        this.sellers = sellers;
        this.users = users;
        this.clock = clock;
    }

    public record SellerReport(int days, long orders, long completed, long rejected, long expired, long cancelled,
                               BigDecimal sales, BigDecimal todaySales, long todayOrders,
                               List<Map.Entry<String, Integer>> popular) {
    }

    public SellerReport forSeller(Seller seller, int days) {
        Instant since = LocalDate.now(clock).minusDays(days - 1L).atStartOfDay(clock.getZone()).toInstant();
        Instant today = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        List<CustomerOrder> list = orders.findBySellerAndPlacedAtAfterOrderByPlacedAtDesc(seller, since);
        Map<OrderStatus, Long> byStatus = list.stream().collect(Collectors.groupingBy(CustomerOrder::getStatus, Collectors.counting()));
        BigDecimal sales = list.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(CustomerOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CustomerOrder> todays = list.stream().filter(o -> !o.getPlacedAt().isBefore(today)).toList();
        BigDecimal todaySales = todays.stream().filter(o -> !o.getStatus().isFailed())
                .map(CustomerOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Integer> counts = new HashMap<>();
        list.stream().filter(o -> !o.getStatus().isFailed())
                .flatMap(o -> o.getItems().stream())
                .forEach(i -> counts.merge(i.getName(), i.getQuantity(), Integer::sum));
        List<Map.Entry<String, Integer>> popular = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5).toList();
        return new SellerReport(days, list.size(),
                byStatus.getOrDefault(OrderStatus.COMPLETED, 0L),
                byStatus.getOrDefault(OrderStatus.REJECTED, 0L),
                byStatus.getOrDefault(OrderStatus.EXPIRED, 0L),
                byStatus.getOrDefault(OrderStatus.CANCELLED, 0L),
                sales, todaySales, todays.size(), popular);
    }

    public record PlatformStats(long customers, long sellersVerified, long sellersPending, long ordersToday,
                                Map<OrderStatus, Long> ordersByStatus) {
    }

    public PlatformStats platform() {
        Instant today = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (Object[] row : orders.countByStatusGrouped()) byStatus.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        return new PlatformStats(users.countByRole(Role.CUSTOMER),
                sellers.countByStatus(SellerStatus.VERIFIED),
                sellers.countByStatus(SellerStatus.PENDING_REVIEW),
                orders.countByPlacedAtAfter(today), byStatus);
    }
}
