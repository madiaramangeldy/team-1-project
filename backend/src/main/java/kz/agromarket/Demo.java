package kz.agromarket;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.stream.Collectors;
import kz.agromarket.analytics.AnalyticsService;
import kz.agromarket.auth.RegisterRequest;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.User;
import kz.agromarket.cart.CartService;
import kz.agromarket.catalog.CatalogFilter;
import kz.agromarket.catalog.Product;
import kz.agromarket.catalog.ProductRequest;
import kz.agromarket.catalog.SeasonalOffer;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.UploadedFile;
import kz.agromarket.delivery.DeliveryCostStrategy;
import kz.agromarket.delivery.DeliveryMethod;
import kz.agromarket.orders.Order;
import kz.agromarket.orders.PlaceOrderRequest;
import kz.agromarket.payments.PaymentMethod;
import kz.agromarket.pricing.PriceQuote;
import kz.agromarket.pricing.PromoCode;

/**
 * 12 user story-дің демонстрациясы.
 * Іске қосу: VS Code-та «Run» батырмасы немесе `mvn compile exec:java`
 */
public class Demo {

    private static final PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final AgroMarketApp app = new AgroMarketApp();
    private static final LocalDate today = LocalDate.now();

    public static void main(String[] args) {
        // ---------------- US-01 ----------------
        title("US-01. Фермердің тіркелуі");
        User aidar = app.auth.register(new RegisterRequest("aidar@farm.kz", "secret123", "Айдар (Алматы обл.)", Role.FARMER));
        User gulnar = app.auth.register(new RegisterRequest("gulnar@farm.kz", "secret123", "Гүлнар (Астана)", Role.FARMER));
        User moderator = app.auth.register(new RegisterRequest("admin@agromarket.kz", "secret123", "Модератор", Role.MODERATOR));
        User cafe = app.auth.register(new RegisterRequest("cafe@mail.kz", "secret123", "«Дастархан» кафесі", Role.BUYER));
        app.auth.uploadDocument(aidar.getId(), new UploadedFile("kualik.pdf", 800_000));
        app.auth.uploadDocument(gulnar.getId(), new UploadedFile("kualik.pdf", 700_000));
        out.println("  ✔ " + aidar.getName() + " тіркелді, мәртебесі: " + aidar.getVerificationStatus());
        out.println("  ✔ " + gulnar.getName() + " тіркелді, мәртебесі: " + gulnar.getVerificationStatus());
        out.println("  Сол email-мен қайта тіркелу:");
        attempt(() -> app.auth.register(new RegisterRequest("aidar@farm.kz", "secret123", "X", Role.FARMER)));
        out.println("  Верификациясыз өнім қосу:");
        attempt(() -> app.catalog.addProduct(aidar.getId(), product("Қызанақ", "Көкөніс", "Алматы облысы", 600, 500).build()));

        // ---------------- US-02 ----------------
        title("US-02. Фермерді верификациялау");
        out.println("  Тексеруді күтіп тұрғандар: " + app.auth.listPendingFarmers(moderator.getId()).size());
        app.auth.approveFarmer(moderator.getId(), aidar.getId());
        app.auth.approveFarmer(moderator.getId(), gulnar.getId());
        out.println("  ✔ Айдар: " + aidar.getVerificationStatus() + ", Гүлнар: " + gulnar.getVerificationStatus());
        out.println("  Айдарға хабарлама: «" + app.notifications.forUser(aidar.getId()).get(0).text() + "»");
        out.println("  Сатып алушы верификация жасамақ болса:");
        attempt(() -> app.auth.approveFarmer(cafe.getId(), aidar.getId()));

        // ---------------- US-03 ----------------
        title("US-03. Өнім қосу");
        Product tomato = app.catalog.addProduct(aidar.getId(), product("Қызанақ", "Көкөніс", "Алматы облысы", 600, 500)
                .organic(true)
                .wholesaleDiscount(10)
                .seasonalOffer(new SeasonalOffer(5, today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31)))
                .build());
        Product apple = app.catalog.addProduct(aidar.getId(), product("Алма", "Жеміс", "Алматы облысы", 450, 300).build());
        Product potato = app.catalog.addProduct(gulnar.getId(), product("Картоп", "Көкөніс", "Астана", 250, 1000)
                .wholesaleDiscount(8).build());
        out.println("  ✔ " + tomato.getName() + " — " + money(tomato.getPricePerKg()) + " ₸/кг, фото: " + tomato.getPhotoUrl());
        out.println("  ✔ " + apple.getName() + ", " + potato.getName() + " қосылды");
        out.println("  Атауы бос өнім:");
        attempt(() -> app.catalog.addProduct(aidar.getId(), product("", "Көкөніс", "Алматы", 100, 10).build()));
        out.println("  Гүлнар Айдардың өнімін өзгертпек болса:");
        attempt(() -> app.catalog.updateProduct(gulnar.getId(), tomato.getId(), product("Қызанақ", "Көкөніс", "Алматы", 1, 1).build()));

        // ---------------- US-04 ----------------
        title("US-04. Каталогты сүзу");
        out.println("  «Көкөніс»: " + names(new CatalogFilter().category("Көкөніс")));
        out.println("  «Көкөніс» + «Алматы облысы» + органик: "
                + names(new CatalogFilter().category("Көкөніс").region("Алматы облысы").organic(true)));
        out.println("  Бағасы 300 ₸-ге дейін: " + names(new CatalogFilter().maxPrice(300)));
        out.println("  «Ет» санаты: " + app.catalog.search(new CatalogFilter().category("Ет")).total()
                + " өнім (қате емес, бос тізім)");

        // ---------------- US-05 ----------------
        title("US-05. Себет (әртүрлі фермерлерден)");
        app.cart.add(cafe.getId(), tomato.getId(), 60);
        app.cart.add(cafe.getId(), apple.getId(), 10);
        app.cart.add(cafe.getId(), potato.getId(), 30);
        CartService.CartView cartView = app.cart.updateQuantity(cafe.getId(), potato.getId(), 40);
        for (CartService.CartLine line : cartView.lines()) {
            out.println("  • " + line.productName() + " " + kg(line.quantityKg()) + " кг → " + money(line.price().total()) + " ₸");
        }
        out.println("  Барлығы: " + kg(cartView.totalWeightKg()) + " кг, " + money(cartView.total()) + " ₸");

        // ---------------- US-06 ----------------
        title("US-06. Көтерме баға — DECORATOR");
        Product tomatoNoSeason = app.catalog.addProduct(aidar.getId(),
                product("Қызанақ (маусымдық жеңілдіксіз)", "Көкөніс", "Алматы облысы", 600, 500).wholesaleDiscount(10).build());
        PriceQuote retail = app.pricing.quote(tomatoNoSeason, 20);
        PriceQuote wholesale = app.pricing.quote(tomatoNoSeason, 60);
        out.println("  Қызанақ 20 кг: " + money(retail.total()) + " ₸ (жеңілдік жоқ, 50 кг-нан аз)");
        out.println("  Қызанақ 60 кг: " + money(wholesale.base()) + " ₸ → " + money(wholesale.total()) + " ₸");
        printDiscounts(wholesale);

        // ---------------- US-07 ----------------
        title("US-07. Маусымдық жеңілдік және промокод — DECORATOR");
        app.pricing.addPromoCode(new PromoCode("AGRO10", 10, today.plusYears(1)));
        app.pricing.addPromoCode(new PromoCode("OLD5", 5, LocalDate.of(2020, 1, 1)));
        PriceQuote full = app.pricing.quote(tomato, 60, "AGRO10", today);
        out.println("  BasePrice → WholesaleDiscount → SeasonalDiscount → PromoCodeDiscount");
        out.println("  Қызанақ 60 кг: " + money(full.base()) + " ₸ → " + money(full.total()) + " ₸");
        printDiscounts(full);
        out.println("  Мерзімі өткен промокод:");
        attempt(() -> app.pricing.quote(tomato, 60, "OLD5", today));

        // ---------------- US-08 ----------------
        title("US-08. Жеткізу тәсілі — STRATEGY + FACTORY METHOD");
        for (DeliveryMethod method : app.deliveryFactory.methods()) {
            DeliveryCostStrategy strategy = app.deliveryFactory.create(method);
            out.println("  " + strategy.title() + ": " + money(strategy.calculate(110, 1200)) + " ₸");
        }

        // ---------------- US-09 ----------------
        title("US-09. Тапсырысты тексеру — CHAIN OF RESPONSIBILITY");
        User student = app.auth.register(new RegisterRequest("buyer@mail.kz", "secret123", "Асқар", Role.BUYER));
        PlaceOrderRequest almatyCourier = new PlaceOrderRequest("Алматы", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CARD, null);
        app.cart.add(student.getId(), apple.getId(), 3);
        out.println("  3 кг алма (минимум 5 кг):");
        attempt(() -> app.orders.placeOrder(student.getId(), almatyCourier));
        app.cart.updateQuantity(student.getId(), apple.getId(), 400);
        out.println("  400 кг алма (қоймада 300 кг):");
        attempt(() -> app.orders.placeOrder(student.getId(), almatyCourier));
        app.cart.updateQuantity(student.getId(), apple.getId(), 10);
        out.println("  10 кг алма, Шымкентке курьер:");
        attempt(() -> app.orders.placeOrder(student.getId(),
                new PlaceOrderRequest("Шымкент", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CARD, null)));
        app.cart.clear(student.getId());

        out.println("  Кафенің тапсырысы (2 фермерден):");
        Order order = app.orders.placeOrder(cafe.getId(),
                new PlaceOrderRequest("Алматы", DeliveryMethod.INTERCITY, 1200, PaymentMethod.CARD, "AGRO10"), today);
        out.println("  ✔ Тапсырыс " + order.getId() + " құрылды, " + order.getSubOrders().size() + " ішкі тапсырысқа бөлінді:");
        for (Order.SubOrder sub : order.getSubOrders()) {
            String items = sub.items().stream().map(i -> i.productName() + " " + kg(i.quantityKg()) + " кг")
                    .collect(Collectors.joining(", "));
            out.println("    • " + app.auth.getUser(sub.farmerId()).getName() + ": " + items + " → " + money(sub.total()) + " ₸");
        }
        out.println("  Өнімдер: " + money(order.getItemsTotal()) + " ₸ + жеткізу: " + money(order.getDeliveryCost())
                + " ₸ = " + money(order.getTotal()) + " ₸");

        // ---------------- US-10 ----------------
        title("US-10. Төлем — ADAPTER + FACTORY METHOD");
        app.orders.pay(order.getId());
        out.println("  Карта (StripeAdapter): мәртебе " + order.getStatus() + ", транзакция " + order.getTransactionId());

        app.cart.add(student.getId(), potato.getId(), 20);
        Order cashOrder = app.orders.placeOrder(student.getId(),
                new PlaceOrderRequest("Алматы", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CASH, null));
        app.orders.pay(cashOrder.getId());
        out.println("  Қолма-қол (CashOnDeliveryAdapter): мәртебе " + cashOrder.getStatus());
        app.orders.confirmCashPayment(cashOrder.getId());
        out.println("  Курьер растағаннан кейін: " + cashOrder.getStatus());
        out.println("  OrdersService екі жағдайда да бірдей: адаптерді фабрика таңдайды.");

        // ---------------- US-11 ----------------
        title("US-11. Төлем туралы хабарлама — OBSERVER");
        for (User farmer : new User[] {aidar, gulnar}) {
            app.notifications.forUser(farmer.getId()).stream().skip(1)
                    .forEach(n -> out.println("  → " + farmer.getName() + ": «" + n.text() + "»"));
        }
        out.println("  Қалдық азайды: қызанақ " + kg(tomato.getStockKg()) + " кг, алма " + kg(apple.getStockKg())
                + " кг, картоп " + kg(potato.getStockKg()) + " кг");

        // ---------------- US-12 ----------------
        title("US-12. Сату дашборды — OBSERVER");
        AnalyticsService.FarmerDashboard dashboard = app.analytics.getFarmerDashboard(aidar.getId(), aidar.getId(), today);
        out.println("  Айдардың түсімі: " + money(dashboard.revenue()) + " ₸");
        out.println("  Ең көп сатылғандар: " + dashboard.topProducts().stream()
                .map(p -> p.productName() + " (" + kg(p.quantityKg()) + " кг)").collect(Collectors.joining(", ")));
        out.println("  Апталық график: " + dashboard.weeklySales().stream()
                .map(d -> d.date().getDayOfMonth() + "." + d.date().getMonthValue() + ": " + money(d.total()))
                .collect(Collectors.joining(" | ")));
        out.println("  Гүлнар Айдардың статистикасын көрмек болса:");
        attempt(() -> app.analytics.getFarmerDashboard(gulnar.getId(), aidar.getId()));

        out.println("\n✔ 12 user story сәтті орындалды.\n");
    }

    // ---------- Көмекші әдістер ----------

    private static ProductRequest.Builder product(String name, String category, String region, double price, double stock) {
        return ProductRequest.builder().name(name).category(category).region(region).pricePerKg(price).stockKg(stock);
    }

    private static String names(CatalogFilter filter) {
        var page = app.catalog.search(filter);
        return page.items().stream().map(Product::getName).collect(Collectors.joining(", "))
                + " (барлығы " + page.total() + ")";
    }

    private static void printDiscounts(PriceQuote quote) {
        quote.discounts().forEach(d -> out.println("    − " + d.label() + ": " + money(d.amount()) + " ₸"));
    }

    private static void attempt(Runnable action) {
        try {
            action.run();
        } catch (DomainException e) {
            out.println("  ✖ Қате [" + e.getCode() + "]: " + e.getMessage());
        }
    }

    private static void title(String text) {
        out.println("\n=== " + text + " ===");
    }

    private static String money(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String kg(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
