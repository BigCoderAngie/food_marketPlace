# Eat.na: Namibian food marketplace

A web marketplace where Windhoek home cooks, food stalls, bakeries and restaurants sell food, and customers order it for pickup or delivery. Built from the *Namibian Food Marketplace SRS v1.0*, plus five changes that came out of the scenario review:

1. **Prep time and pickup slots.** Every food item has a prep time, and customers pick a pickup time at checkout.
2. **Today's portions.** Sellers set how many portions they have today. The count resets every morning, and the item shows "Sold out" automatically when it runs out.
3. **Accept countdown.** Sellers have a set number of minutes to accept an order (10 by default). Both sides see the countdown. When it runs out, or the seller rejects the order, the order closes and the customer is shown similar sellers nearby.
4. **Landmark delivery.** Delivery addresses are suburb + landmark + phone, and each seller sets a flat fee per suburb.
5. **Cash and eWallet/EFT payments.** The seller confirms the money arrived, and payment status is tracked separately from order status.

## Run it

You need **Java 21**. Nothing else: by default the app uses an embedded H2 database stored in `./data`.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080. The first start loads demo sellers and menus for Windhoek.

| Role | Login | Password |
|---|---|---|
| Customer | `demo@eat.na` | `demo12345` |
| Seller | `meme@eat.na` (also `kapana@`, `bakery@`, `lunch@`, `fish@`) | `seller1234` |
| Admin | `admin@eat.na` | `admin1234` |

`fish@eat.na` (Swakop Fish Shack) starts out unverified, so you can try the admin verification flow.

To start from an empty database, stop the app and delete the `data/` folder. To turn off demo data, set `eatna.seed-demo-data=false`.

### Using MySQL

```sql
CREATE DATABASE eatna CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
DB_USER=root DB_PASSWORD=secret ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

The `DB_HOST`, `DB_PORT` and `DB_NAME` settings are also available (see `application-mysql.properties`). Tables are created automatically.

### Tests

```bash
./mvnw test
```

The tests cover the order state machine, the full order loop through to the review, portion overselling, expiry, the eWallet confirm-and-refund flow, delivery suburbs, isolation between sellers, and role-based access.

## What's in it

**Customers** (browsing needs no account)
- A home page, search with filters (category, suburb, seller type, pickup/delivery, max price, available now), food pages and seller pages with shareable URLs (`/s/<seller>`).
- A cart that holds one seller's food, then checkout: pickup time or delivery by suburb + landmark, and payment by cash, eWallet or EFT.
- An order page showing the live status, the accept countdown, payment instructions and an "I've sent the money" button, the order history, and alternative sellers if the order fails.
- Confirming that the food arrived, a review once the order is completed, and in-app notifications.

**Sellers**
- Sign up, after which an admin has to verify them before they go public. They can build their menu while they wait.
- A dashboard with new orders, their countdowns and one-tap Accept. It refreshes itself.
- On each order: accept, reject (with a reason), prepare, ready, collected or out for delivery, delivered, complete, cancel (with a reason), confirm payment and mark refunds.
- A menu with photos, prep time, daily portions, allergens and tags, plus quick controls for sold out, available or hidden.
- Profile, pickup directions, payment instructions, delivery suburbs and fees, the accept time limit, and pausing the storefront.
- Reviews and reports: orders, sales, most ordered, and rejected/expired/cancelled counts.

**Admins**
- A dashboard, seller verification (verify, reject, suspend, reinstate), orders, resolving delivery problems, review moderation, categories, users, the audit log and the notification log.

## How it's built

- **Spring Boot 4.1** (Spring MVC, Spring Security, Spring Data JPA), with **Thymeleaf** pages rendered on the server and a small amount of JavaScript. The pages are mobile-first and light enough for slow connections.
- **Business rules live in the services**, never in pages (SRS 9.1):
  - `OrderService`: the only place an order's status changes. It checks the allowed transitions in `OrderStatus`, records an order event and an audit entry, reserves and releases portions, and keeps the payment status in step.
  - `CartService`: one seller per cart, rechecking prices and availability.
  - `OrderExpiryJob`: expires unanswered orders every 30 seconds.
- **Order placement** locks the food rows, so two customers can't both buy the last portion.
- **Item names and prices are copied into each order**, so menu edits never change past orders.
- **WhatsApp/SMS alerts** go through `NotificationService`. Until a provider is connected they are recorded as `LOGGED` (see Admin, Audit & notifications). Set `eatna.notifications.whatsapp-enabled=true` and add the provider call there.
- **Food photos** are saved to `./uploads`. Move them to object storage before going to production.

```
src/main/java/na/eat/marketplace
├── config/   security, time zone (Africa/Windhoek), formatting, demo data
├── domain/   entities and enums (OrderStatus holds the state machine)
├── repo/     Spring Data repositories
├── service/  business rules
└── web/      controllers: shop, cart/checkout, customer orders, seller/, admin/
```

## Still open (SRS section 15)

- **Online card payments.** No gateway yet. The `Payment` record already has a reference field and is kept separate from the order status.
- **Real WhatsApp/SMS sending, and phone OTP at sign-up.**
- **Commission and subscription plans, and featured listings.**
- **Delivery partners (P3).** The order record doesn't need to change for these.
- **Before a production launch:** database migrations (Flyway), and photo storage outside the server.
