package kz.agromarket;

import static kz.agromarket.TestSupport.assertMoney;
import static kz.agromarket.TestSupport.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import kz.agromarket.analytics.AnalyticsService;
import kz.agromarket.catalog.Product;
import kz.agromarket.common.DomainException;
import kz.agromarket.delivery.DeliveryCostStrategy;
import kz.agromarket.delivery.DeliveryMethod;
import kz.agromarket.delivery.DeliveryStrategyFactory;
import kz.agromarket.orders.Order;
import kz.agromarket.orders.PlaceOrderRequest;
import kz.agromarket.payments.CashOnDeliveryAdapter;
import kz.agromarket.payments.PaymentGateway;
import kz.agromarket.payments.PaymentGatewayFactory;
import kz.agromarket.payments.PaymentMethod;
import kz.agromarket.payments.StripeAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-08 – US-12. Жеткізу, тапсырыс, төлем, оқиғалар")
class OrdersTest {

    private final TestSupport t = new TestSupport();
    private static final PlaceOrderRequest ALMATY_CARD =
            new PlaceOrderRequest("Алматы", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CARD, null);
    private static final PlaceOrderRequest ALMATY_CASH =
            new PlaceOrderRequest("Алматы", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CASH, null);

    // ---------- US-08: Strategy + Factory Method ----------

    @Test
    @DisplayName("US-08: әр жеткізу тәсілі өз формуласымен есептейді")
    void deliveryStrategies() {
        DeliveryStrategyFactory factory = DeliveryStrategyFactory.withDefaults();
        assertMoney(0, factory.create(DeliveryMethod.PICKUP).calculate(100, 1000));
        assertMoney(1500, factory.create(DeliveryMethod.CITY_COURIER).calculate(100, 1000));
        assertMoney(2000 + 100 * 30 + 1000 * 15, factory.create(DeliveryMethod.INTERCITY).calculate(100, 1000));
    }

    @Test
    @DisplayName("US-08: жаңа тәсіл бар кодты өзгертпей бір класпен қосылады")
    void newStrategyWithoutChangingCode() {
        DeliveryCostStrategy drone = new DeliveryCostStrategy() {
            public String title() { return "Дрон"; }
            public BigDecimal calculate(double weightKg, double distanceKm) { return BigDecimal.valueOf(999); }
        };
        DeliveryStrategyFactory factory = new DeliveryStrategyFactory().register(DeliveryMethod.PICKUP, () -> drone);
        assertMoney(999, factory.create(DeliveryMethod.PICKUP).calculate(1, 1));
    }

    @Test
    @DisplayName("US-08: тіркелмеген тәсіл — қате")
    void unknownDeliveryMethod() {
        assertThrows(DomainException.class, () -> new DeliveryStrategyFactory().create(DeliveryMethod.INTERCITY));
    }

    // ---------- US-09: Chain of Responsibility ----------

    @Test
    @DisplayName("US-09: минималды салмақтан аз тапсырысты тізбек тоқтатады")
    void minWeightStopsChain() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        t.app.cart.add(t.buyer, p.getId(), 3);
        DomainException e = assertThrows(DomainException.class, () -> t.app.orders.placeOrder(t.buyer, ALMATY_CARD));
        assertEquals("MIN_WEIGHT", e.getCode());
        assertEquals("Минималды салмақ — 5 кг. Сіздің тапсырысыңыз: 3 кг", e.getMessage());
    }

    @Test
    @DisplayName("US-09: қоймада жеткіліксіз болса — бірінші буын тоқтатады")
    void stockCheckFirst() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().stockKg(10).build());
        t.app.cart.add(t.buyer, p.getId(), 20);
        DomainException e = assertThrows(DomainException.class, () -> t.app.orders.placeOrder(t.buyer, ALMATY_CARD));
        assertEquals("OUT_OF_STOCK", e.getCode());
    }

    @Test
    @DisplayName("US-09: жеткізу жоқ өңір — қате")
    void unsupportedRegion() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        t.app.cart.add(t.buyer, p.getId(), 10);
        PlaceOrderRequest shymkent = new PlaceOrderRequest("Шымкент", DeliveryMethod.CITY_COURIER, 10, PaymentMethod.CARD, null);
        DomainException e = assertThrows(DomainException.class, () -> t.app.orders.placeOrder(t.buyer, shymkent));
        assertEquals("REGION_NOT_SUPPORTED", e.getCode());
    }

    @Test
    @DisplayName("US-09: тапсырыс фермерлер бойынша ішкі тапсырыстарға бөлінеді")
    void splitIntoSubOrders() {
        Product a = t.app.catalog.addProduct(t.farmerA, product().build());
        Product b = t.app.catalog.addProduct(t.farmerB, product().name("Картоп").build());
        t.app.cart.add(t.buyer, a.getId(), 10);
        t.app.cart.add(t.buyer, b.getId(), 10);

        Order order = t.app.orders.placeOrder(t.buyer, ALMATY_CARD);

        Set<String> farmers = order.getSubOrders().stream().map(Order.SubOrder::farmerId).collect(Collectors.toSet());
        assertEquals(Set.of(t.farmerA, t.farmerB), farmers);
        assertTrue(t.app.cart.items(t.buyer).isEmpty());
    }

    // ---------- US-10: Adapter + Factory Method ----------

    @Test
    @DisplayName("US-10: фабрика төлем тәсіліне қарай керекті адаптерді жасайды")
    void factoryCreatesAdapters() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(PaymentGatewayFactory.CardProvider.STRIPE);
        assertInstanceOf(StripeAdapter.class, factory.create(PaymentMethod.CARD));
        assertInstanceOf(CashOnDeliveryAdapter.class, factory.create(PaymentMethod.CASH));
    }

    @Test
    @DisplayName("US-10: екі адаптер де бірдей PaymentResult қайтарады")
    void sameResultType() {
        PaymentGatewayFactory factory = new PaymentGatewayFactory(PaymentGatewayFactory.CardProvider.STRIPE);
        PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest("o1", BigDecimal.valueOf(1000), "KZT");
        assertEquals(PaymentGateway.Status.PAID, factory.create(PaymentMethod.CARD).pay(request).status());
        assertEquals(PaymentGateway.Status.PENDING, factory.create(PaymentMethod.CASH).pay(request).status());
    }

    @Test
    @DisplayName("US-10: карта арқылы төлегенде тапсырыс PAID болады")
    void cardPayment() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        t.app.cart.add(t.buyer, p.getId(), 10);
        Order order = t.app.orders.pay(t.app.orders.placeOrder(t.buyer, ALMATY_CARD).getId());
        assertEquals(Order.Status.PAID, order.getStatus());
        assertTrue(order.getTransactionId().startsWith("pi_test_"));
    }

    @Test
    @DisplayName("US-10: қолма-қол төлем курьер растағаннан кейін PAID болады")
    void cashPayment() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        t.app.cart.add(t.buyer, p.getId(), 10);
        Order order = t.app.orders.placeOrder(t.buyer, ALMATY_CASH);
        t.app.orders.pay(order.getId());
        assertEquals(Order.Status.AWAITING_CASH, order.getStatus());
        t.app.orders.confirmCashPayment(order.getId());
        assertEquals(Order.Status.PAID, order.getStatus());
    }

    // ---------- US-11, US-12: Observer ----------

    private Product[] payTwoFarmerOrder() {
        Product a = t.app.catalog.addProduct(t.farmerA, product().stockKg(100).pricePerKg(100).build());
        Product b = t.app.catalog.addProduct(t.farmerB, product().name("Картоп").stockKg(100).pricePerKg(50).build());
        t.app.cart.add(t.buyer, a.getId(), 10);
        t.app.cart.add(t.buyer, b.getId(), 20);
        t.app.orders.pay(t.app.orders.placeOrder(t.buyer, ALMATY_CARD).getId());
        return new Product[] {a, b};
    }

    @Test
    @DisplayName("US-11: әр фермер өз ішкі тапсырысы туралы хабарлама алады")
    void farmersNotified() {
        payTwoFarmerOrder();
        // 1-хабарлама — верификация туралы, 2-сі — тапсырыс туралы
        assertTrue(t.app.notifications.forUser(t.farmerA).get(1).text().contains("10 кг"));
        assertTrue(t.app.notifications.forUser(t.farmerB).get(1).text().contains("20 кг"));
    }

    @Test
    @DisplayName("US-11: өнім қалдығы автоматты азаяды")
    void stockDecreased() {
        Product[] products = payTwoFarmerOrder();
        assertEquals(90, products[0].getStockKg());
        assertEquals(80, products[1].getStockKg());
    }

    @Test
    @DisplayName("US-11: қолма-қол төлемде курьер растағанша оқиға жарияланбайды")
    void noEventBeforeCashConfirmed() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().stockKg(100).build());
        t.app.cart.add(t.buyer, p.getId(), 10);
        Order order = t.app.orders.placeOrder(t.buyer, ALMATY_CASH);
        t.app.orders.pay(order.getId());
        assertEquals(100, p.getStockKg());
        t.app.orders.confirmCashPayment(order.getId());
        assertEquals(90, p.getStockKg());
    }

    @Test
    @DisplayName("US-12: дашборд оқиға арқылы жиналған түсімді көрсетеді")
    void dashboard() {
        payTwoFarmerOrder();
        AnalyticsService.FarmerDashboard d = t.app.analytics.getFarmerDashboard(t.farmerA, t.farmerA);
        assertMoney(1000, d.revenue());
        assertEquals("Қызанақ", d.topProducts().get(0).productName());
        assertEquals(7, d.weeklySales().size());
    }

    @Test
    @DisplayName("US-12: фермер басқа фермердің статистикасын көре алмайды")
    void dashboardIsPrivate() {
        payTwoFarmerOrder();
        DomainException e = assertThrows(DomainException.class,
                () -> t.app.analytics.getFarmerDashboard(t.farmerB, t.farmerA));
        assertEquals(403, e.getStatus());
    }
}
