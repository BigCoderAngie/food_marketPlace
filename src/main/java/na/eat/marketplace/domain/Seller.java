package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", unique = true)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    /** Used in shareable URLs, e.g. /s/meme-ndapandulas-kitchen (FR-SR-06). */
    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerType type = SellerType.HOME_COOK;

    @Column(length = 1000)
    private String description;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String whatsapp;

    @Column(nullable = false, length = 60)
    private String town = "Windhoek";

    @Column(nullable = false, length = 80)
    private String suburb;

    /** Landmark-style directions shown to customers once an order is accepted (FR-DL-02). */
    @Column(length = 300)
    private String pickupInstructions;

    @Column(length = 200)
    private String openingHours;

    /** Where customers send eWallet/EFT payments. */
    @Column(length = 300)
    private String paymentInstructions;

    private boolean offersPickup = true;

    private boolean offersDelivery = false;

    /** FR-SL-05: storefront paused without deleting the account. */
    private boolean paused = false;

    /** Minutes the seller has to accept an order before it expires. */
    private int acceptWindowMinutes = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status = SellerStatus.PENDING_REVIEW;

    @Column(length = 300)
    private String statusNote;

    private Instant verifiedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("suburb")
    private List<DeliveryZone> deliveryZones = new ArrayList<>();

    /** Aggregates refreshed when reviews change (FR-RV-05). */
    @Column(precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    private int ratingCount;

    private int completedOrders;

    public boolean isPublic() {
        return status == SellerStatus.VERIFIED && !paused;
    }

    public boolean isOpenForOrders() {
        return isPublic() && owner != null && owner.getStatus().canLogIn();
    }

    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0))) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
            if (sb.length() == 2) break;
        }
        return sb.isEmpty() ? "?" : sb.toString();
    }
}
