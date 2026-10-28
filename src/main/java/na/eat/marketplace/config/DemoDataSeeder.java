package na.eat.marketplace.config;

import na.eat.marketplace.domain.*;
import na.eat.marketplace.repo.CategoryRepository;
import na.eat.marketplace.repo.FoodItemRepository;
import na.eat.marketplace.repo.UserRepository;
import na.eat.marketplace.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo content for Windhoek so the marketplace isn't empty on first run. Only runs on an empty
 * database. Turn off with eatna.seed-demo-data=false.
 */
@Component
@ConditionalOnProperty(name = "eatna.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final FoodItemRepository foods;
    private final AccountService accounts;
    private final SellerService sellers;
    private final OrderService orders;
    private final ReviewService reviews;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Map<String, Category> cats = new HashMap<>();

    public DemoDataSeeder(UserRepository users, CategoryRepository categories, FoodItemRepository foods,
                          AccountService accounts, SellerService sellers, OrderService orders, ReviewService reviews,
                          PasswordEncoder encoder, TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.categories = categories;
        this.foods = foods;
        this.accounts = accounts;
        this.sellers = sellers;
        this.orders = orders;
        this.reviews = reviews;
        this.encoder = encoder;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        tx.executeWithoutResult(s -> seed());
        log.info("Demo data loaded. Admin: admin@eat.na / admin1234, customer: demo@eat.na / demo12345, sellers: <name>@eat.na / seller1234");
    }

    private void seed() {
        User admin = new User();
        admin.setName("Platform Admin");
        admin.setEmail("admin@eat.na");
        admin.setPasswordHash(encoder.encode("admin1234"));
        admin.setRole(Role.ADMIN);
        users.save(admin);

        category("Traditional", "🍲", 1);
        category("Kapana & braai", "🍖", 2);
        category("Vetkoek & fat cakes", "🥯", 3);
        category("Lunch meals", "🍱", 4);
        category("Fish & seafood", "🐟", 5);
        category("Bakery & sweets", "🥐", 6);
        category("Vegetarian", "🥗", 7);
        category("Drinks", "🥤", 8);

        User demo = accounts.registerCustomer("Demo Customer", "demo@eat.na", "081 111 1111", "demo12345");

        Seller meme = seller("Ndapandula Shikongo", "meme@eat.na", "081 222 3344", "Meme Ndapandula's Kitchen",
                SellerType.HOME_COOK, "Katutura",
                "Home-cooked Oshiwambo food made fresh every morning. Oshifima, ekaka and slow-cooked beef stew.",
                "Green gate next to the Eveline Street car wash. Call when you arrive.", "Mon–Sat 10:00–19:00",
                "MTC eWallet / Blue Wallet to 081 222 3344, use your order number as reference.", true, true);
        zone(meme, "Katutura", "15");
        zone(meme, "Wanaheda", "20");
        zone(meme, "Okuryangava", "25");
        food(meme, "Traditional", "Oshifima with beef stew", "Stiff mahangu porridge with slow-cooked beef stew and a side of ekaka.", "55", 15, 30, "", "halal");
        food(meme, "Traditional", "Mahangu porridge with omaere", "Soft mahangu porridge with omaere (sour milk).", "35", 10, 20, "Milk", "vegetarian");
        food(meme, "Traditional", "Ekaka (wild spinach) side", "Dried wild spinach cooked with tomato and onion.", "20", 5, null, "", "vegetarian,vegan");
        food(meme, "Kapana & braai", "Chicken feet & pap", "Spicy braised chicken feet with pap.", "40", 15, 15, "", "spicy");

        Seller kapana = seller("Tuhafeni Nghipondoka", "kapana@eat.na", "081 333 4455", "Kapana Corner Grill",
                SellerType.FOOD_TRUCK, "Katutura",
                "Flame-grilled kapana at Single Quarters, with fresh salsa and chilli salt.",
                "Stall 14, Single Quarters market. Look for the red umbrella.", "Daily 11:00–22:00",
                null, true, false);
        food(kapana, "Kapana & braai", "Kapana (N$ 40 portion)", "Grilled beef strips with salsa and chilli salt.", "40", 10, 60, "", "spicy");
        food(kapana, "Kapana & braai", "Boerewors roll", "Grilled boerewors in a fresh roll with tomato relish.", "35", 10, null, "Gluten", "");
        food(kapana, "Vetkoek & fat cakes", "Fat cakes (6)", "Six golden fat cakes, still warm.", "20", 5, 40, "Gluten", "vegetarian");
        food(kapana, "Drinks", "Oshikundu (500 ml)", "Traditional fermented mahangu drink.", "15", 0, 25, "", "vegan");

        Seller bakery = seller("Anna Becker", "bakery@eat.na", "081 444 5566", "Klein Windhoek Bakery",
                SellerType.BAKERY, "Klein Windhoek",
                "German-Namibian bakery: fresh Brötchen, apple strudel, pies and vetkoek every morning from 06:30.",
                "Corner of Nelson Mandela Ave and Schanzen Rd, next to the pharmacy.", "Mon–Fri 06:30–17:00, Sat 07:00–13:00",
                "EFT to Bank Windhoek 8012345678 (A Becker), reference = order number.", true, true);
        zone(bakery, "Klein Windhoek", "20");
        zone(bakery, "Eros", "25");
        zone(bakery, "Windhoek CBD", "30");
        food(bakery, "Bakery & sweets", "Apple strudel slice", "Classic strudel with cream.", "38", 5, 24, "Gluten, dairy", "vegetarian");
        food(bakery, "Bakery & sweets", "Brötchen (6)", "Six crusty rolls, baked this morning.", "30", 0, 50, "Gluten", "vegan");
        food(bakery, "Vetkoek & fat cakes", "Vetkoek with curried mince", "Large vetkoek filled with mild curried mince.", "45", 10, 30, "Gluten", "");
        food(bakery, "Lunch meals", "Chicken & mushroom pie", "Flaky pastry pie, made in-house.", "42", 10, 20, "Gluten, dairy", "");

        Seller lunch = seller("Johannes Gariseb", "lunch@eat.na", "081 555 6677", "CBD Lunch Box",
                SellerType.RESTAURANT, "Windhoek CBD",
                "Quick office lunches near Independence Avenue. Order ahead and skip the queue.",
                "Ground floor, Sanlam Centre, Independence Ave.", "Mon–Fri 10:30–15:00",
                "Blue Wallet or EasyWallet to 081 555 6677.", true, true);
        zone(lunch, "Windhoek CBD", "15");
        zone(lunch, "Windhoek West", "20");
        zone(lunch, "Klein Windhoek", "25");
        food(lunch, "Lunch meals", "Grilled chicken, rice & salad", "Quarter chicken, savoury rice and garden salad.", "75", 20, 40, "", "halal");
        food(lunch, "Fish & seafood", "Hake & chips", "Walvis Bay hake fried in a light batter with chips.", "85", 20, 25, "Fish, gluten", "");
        food(lunch, "Vegetarian", "Veggie wrap", "Grilled veg, hummus and feta in a wrap.", "60", 10, 20, "Dairy, gluten, sesame", "vegetarian");
        food(lunch, "Drinks", "Fresh orange juice", "Freshly squeezed, 350 ml.", "25", 5, null, "", "vegan");

        // A seller waiting for admin verification, to show that flow.
        Seller fish = seller("Maria Nangolo", "fish@eat.na", "081 666 7788", "Swakop Fish Shack",
                SellerType.RESTAURANT, "Vineta",
                "Fresh fish and chips by the beach in Swakopmund.", null, "Daily 11:00–20:00", null, true, false);
        fish.setTown("Swakopmund");
        food(fish, "Fish & seafood", "Kingklip & chips", "Line-caught kingklip, grilled or fried.", "120", 20, 20, "Fish", "");

        for (Seller s : new Seller[]{meme, kapana, bakery, lunch}) sellers.verify(s.getId(), "seed");

        // One finished order with a review so ratings show.
        Cart cart = new Cart();
        FoodItem stew = foods.findBySellerAndDeletedFalseOrderByCategory_SortOrderAscNameAsc(meme).getFirst();
        cart.put(meme.getId(), stew.getId(), 2);
        CheckoutForm form = new CheckoutForm();
        form.setFulfilment(FulfilmentMethod.PICKUP);
        form.setPickupTime(orders.earliestPickup(stew.getPrepMinutes()));
        form.setContactPhone("081 111 1111");
        form.setPaymentMethod(PaymentMethod.CASH);
        CustomerOrder o = orders.place(demo, cart, form);
        orders.accept(meme, o.getCode());
        orders.startPreparing(meme, o.getCode());
        orders.markReady(meme, o.getCode());
        orders.handOver(meme, o.getCode());
        orders.confirmReceived(demo, o.getCode());
        reviews.submit(demo, o.getCode(), 5, "Best oshifima in Katutura. Ready exactly when promised.");
    }

    private void category(String name, String icon, int order) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(Slugs.of(name));
        c.setIcon(icon);
        c.setSortOrder(order);
        cats.put(name, categories.save(c));
    }

    private Seller seller(String owner, String email, String phone, String name, SellerType type, String suburb,
                          String description, String pickup, String hours, String payment,
                          boolean offersPickup, boolean offersDelivery) {
        Seller s = accounts.registerSeller(owner, email, phone, "seller1234", name, type, suburb, description);
        s.setPickupInstructions(pickup);
        s.setOpeningHours(hours);
        s.setPaymentInstructions(payment);
        s.setOffersPickup(offersPickup);
        s.setOffersDelivery(offersDelivery);
        return s;
    }

    private void zone(Seller s, String suburb, String fee) {
        sellers.addZone(s, suburb, new BigDecimal(fee), "seed");
    }

    private void food(Seller s, String category, String name, String description, String price, int prep,
                      Integer portions, String allergens, String tags) {
        FoodItem f = new FoodItem();
        f.setSeller(s);
        f.setCategory(cats.get(category));
        f.setName(name);
        f.setDescription(description);
        f.setPrice(new BigDecimal(price).setScale(2));
        f.setPrepMinutes(prep);
        f.setDailyPortions(portions);
        f.setPortionsDate(LocalDate.now(clock));
        f.setAllergens(allergens == null || allergens.isBlank() ? null : allergens);
        f.setDietaryTags(tags == null || tags.isBlank() ? null : tags);
        foods.save(f);
    }
}
