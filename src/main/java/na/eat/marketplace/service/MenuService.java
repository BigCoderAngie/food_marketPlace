package na.eat.marketplace.service;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.CategoryRepository;
import na.eat.marketplace.repo.FoodItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Food listings, menus and daily availability (FR-FD-01..07). */
@Service
public class MenuService {

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    private final FoodItemRepository foods;
    private final CategoryRepository categories;
    private final AuditService audit;
    private final Clock clock;
    private final Path uploadDir;

    public MenuService(FoodItemRepository foods, CategoryRepository categories, AuditService audit, Clock clock,
                       @Value("${eatna.upload-dir:uploads}") String uploadDir) {
        this.foods = foods;
        this.categories = categories;
        this.audit = audit;
        this.clock = clock;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public List<FoodItem> menu(Seller seller) {
        return foods.findBySellerAndDeletedFalseOrderByCategory_SortOrderAscNameAsc(seller);
    }

    /** Fetch an item and check it belongs to this seller (FR-FD-04: sellers edit only their own). */
    public FoodItem own(Seller seller, Long foodId) {
        FoodItem f = foods.findById(foodId).filter(x -> !x.isDeleted())
                .orElseThrow(() -> new NotFoundException("Food item not found"));
        if (!f.getSeller().getId().equals(seller.getId())) throw new NotFoundException("Food item not found");
        return f;
    }

    @Transactional
    public FoodItem save(Seller seller, Long foodId, FoodForm form, MultipartFile photo, String actor) {
        FoodItem f = foodId == null ? new FoodItem() : own(seller, foodId);
        boolean isNew = foodId == null;
        String before = isNew ? null : f.getPrice() + "/" + f.getStatus() + "/" + f.getDailyPortions();

        f.setSeller(seller);
        f.setName(form.getName().trim());
        f.setCategory(form.getCategoryId() == null ? null : categories.findById(form.getCategoryId()).orElse(null));
        f.setDescription(blankToNull(form.getDescription()));
        f.setPrice(form.getPrice().setScale(2, RoundingMode.HALF_UP));
        f.setPrepMinutes(form.getPrepMinutes());
        f.setStatus(form.getStatus() == null ? FoodStatus.AVAILABLE : form.getStatus());
        f.setIngredients(blankToNull(form.getIngredients()));
        f.setAllergens(blankToNull(form.getAllergens()));
        f.setDietaryTags(blankToNull(form.getDietaryTags()));
        applyPortions(f, form.getDailyPortions());
        if (photo != null && !photo.isEmpty()) f.setPhotoPath(storePhoto(photo));
        foods.save(f);

        String after = f.getPrice() + "/" + f.getStatus() + "/" + f.getDailyPortions();
        audit.log(actor, isNew ? "FOOD_CREATED" : "FOOD_UPDATED", "FoodItem", f.getId(),
                isNew ? f.getName() + " " + after : before + " -> " + after);
        return f;
    }

    /** Quick availability controls from the menu list (SRS 7.2 "Availability"). */
    @Transactional
    public void setStatus(Seller seller, Long foodId, FoodStatus status, String actor) {
        FoodItem f = own(seller, foodId);
        FoodStatus before = f.getStatus();
        f.setStatus(status);
        audit.log(actor, "FOOD_STATUS", "FoodItem", f.getId(), before + " -> " + status);
    }

    /** Set today's portions. Resets today's count, so it means "this many left from now". */
    @Transactional
    public void setTodaysPortions(Seller seller, Long foodId, Integer portions, String actor) {
        FoodItem f = own(seller, foodId);
        if (portions != null && portions < 0) throw new BusinessException("Portions can't be negative.");
        f.setDailyPortions(portions);
        f.setPortionsSold(0);
        f.setPortionsDate(today());
        if (portions != null && portions > 0 && f.getStatus() == FoodStatus.SOLD_OUT) f.setStatus(FoodStatus.AVAILABLE);
        audit.log(actor, "FOOD_PORTIONS", "FoodItem", f.getId(), String.valueOf(portions));
    }

    @Transactional
    public void delete(Seller seller, Long foodId, String actor) {
        FoodItem f = own(seller, foodId);
        // Soft delete: order history keeps pointing at the item.
        f.setDeleted(true);
        audit.log(actor, "FOOD_DELETED", "FoodItem", f.getId(), f.getName());
    }

    private void applyPortions(FoodItem f, Integer newLimit) {
        if (newLimit == null) {
            f.setDailyPortions(null);
            return;
        }
        if (!today().equals(f.getPortionsDate())) {
            f.setPortionsDate(today());
            f.setPortionsSold(0);
        }
        f.setDailyPortions(newLimit);
    }

    private String storePhoto(MultipartFile photo) {
        String ext = IMAGE_TYPES.get(photo.getContentType());
        if (ext == null) throw new BusinessException("Upload a JPG, PNG or WebP photo.");
        if (photo.getSize() > 5 * 1024 * 1024) throw new BusinessException("Photos must be under 5 MB.");
        try {
            Files.createDirectories(uploadDir.resolve("food"));
            String name = "food/" + UUID.randomUUID() + ext;
            try (InputStream in = photo.getInputStream()) {
                Files.copy(in, uploadDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new BusinessException("The photo couldn't be saved. Please try again.");
        }
    }

    // --- Categories (admin, FR-AD-01) ---

    public List<Category> activeCategories() {
        return categories.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public List<Category> allCategories() {
        return categories.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional
    public void addCategory(String name, String icon, String actor) {
        if (name == null || name.isBlank()) throw new BusinessException("Enter a category name.");
        if (categories.existsByNameIgnoreCase(name.trim())) throw new BusinessException("That category already exists.");
        Category c = new Category();
        c.setName(name.trim());
        c.setSlug(Slugs.of(name));
        c.setIcon(blankToNull(icon));
        c.setSortOrder(categories.findAll().size() + 1);
        categories.save(c);
        audit.log(actor, "CATEGORY_ADDED", "Category", c.getId(), c.getName());
    }

    @Transactional
    public void toggleCategory(Long id, String actor) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        c.setActive(!c.isActive());
        audit.log(actor, c.isActive() ? "CATEGORY_ENABLED" : "CATEGORY_DISABLED", "Category", c.getId(), c.getName());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
