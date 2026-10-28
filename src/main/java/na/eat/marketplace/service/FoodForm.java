package na.eat.marketplace.service;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import na.eat.marketplace.domain.FoodItem;
import na.eat.marketplace.domain.FoodStatus;

import java.math.BigDecimal;

@Getter
@Setter
public class FoodForm {

    @NotBlank(message = "Enter a name.")
    @Size(max = 120)
    private String name;

    private Long categoryId;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "Enter a price.")
    @DecimalMin(value = "1.00", message = "Price must be at least N$ 1.")
    @DecimalMax(value = "100000", message = "That price is too high.")
    private BigDecimal price;

    @Min(value = 0, message = "Prep time can't be negative.")
    @Max(value = 600, message = "Prep time must be under 10 hours.")
    private int prepMinutes = 20;

    @Min(value = 0, message = "Portions can't be negative.")
    private Integer dailyPortions;

    private FoodStatus status = FoodStatus.AVAILABLE;

    @Size(max = 300)
    private String ingredients;

    @Size(max = 200)
    private String allergens;

    @Size(max = 200)
    private String dietaryTags;

    public static FoodForm from(FoodItem f) {
        FoodForm form = new FoodForm();
        form.name = f.getName();
        form.categoryId = f.getCategory() == null ? null : f.getCategory().getId();
        form.description = f.getDescription();
        form.price = f.getPrice();
        form.prepMinutes = f.getPrepMinutes();
        form.dailyPortions = f.getDailyPortions();
        form.status = f.getStatus();
        form.ingredients = f.getIngredients();
        form.allergens = f.getAllergens();
        form.dietaryTags = f.getDietaryTags();
        return form;
    }
}
