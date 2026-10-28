package na.eat.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 300)
    private String photoPath;

    /** Minutes needed to prepare once accepted (SRS change #1). */
    private int prepMinutes = 20;

    /**
     * "Today's portions" (SRS change #2). Null means no daily limit.
     * {@link #portionsSold} counts against this and resets when {@link #portionsDate} is not today.
     */
    private Integer dailyPortions;

    private int portionsSold;

    private LocalDate portionsDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodStatus status = FoodStatus.AVAILABLE;

    @Column(length = 300)
    private String ingredients;

    @Column(length = 200)
    private String allergens;

    /** Comma-separated, e.g. "vegetarian,halal,spicy" (FR-FD-05). */
    @Column(length = 200)
    private String dietaryTags;

    private boolean deleted = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Portions left today, or null when unlimited. */
    public Integer portionsLeft(LocalDate today) {
        if (dailyPortions == null) return null;
        int sold = today.equals(portionsDate) ? portionsSold : 0;
        return Math.max(0, dailyPortions - sold);
    }

    public boolean isOrderable(LocalDate today) {
        if (deleted || status != FoodStatus.AVAILABLE) return false;
        Integer left = portionsLeft(today);
        return left == null || left > 0;
    }

    public String[] getTagList() {
        if (dietaryTags == null || dietaryTags.isBlank()) return new String[0];
        return java.util.Arrays.stream(dietaryTags.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }
}
