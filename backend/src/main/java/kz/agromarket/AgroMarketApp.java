package kz.agromarket;

import java.util.List;
import kz.agromarket.analytics.AnalyticsService;
import kz.agromarket.auth.AuthService;
import kz.agromarket.cart.CartService;
import kz.agromarket.catalog.CatalogService;
import kz.agromarket.catalog.PhotoStorage;
import kz.agromarket.common.EventBus;
import kz.agromarket.delivery.DeliveryStrategyFactory;
import kz.agromarket.notifications.NotificationService;
import kz.agromarket.orders.OrderChecks;
import kz.agromarket.orders.OrdersService;
import kz.agromarket.payments.PaymentGatewayFactory;
import kz.agromarket.pricing.PricingService;

/**
 * Барлық модульдерді біріктіретін жер (composition root).
 * Әр сервис өзіне керекті тәуелділіктерді конструктор арқылы алады (Dependency Injection).
 * Spring Boot-та бұл жұмысты @Service, @Configuration және DI-контейнер атқарады.
 */
public class AgroMarketApp {

    public static final List<String> SUPPORTED_REGIONS = List.of("Алматы", "Астана", "Алматы облысы");

    public final EventBus eventBus = new EventBus();
    public final AuthService auth = new AuthService(eventBus);
    public final CatalogService catalog = new CatalogService(auth, new PhotoStorage.InMemory());
    public final PricingService pricing = new PricingService();
    public final CartService cart = new CartService(catalog, pricing);
    public final DeliveryStrategyFactory deliveryFactory = DeliveryStrategyFactory.withDefaults();
    public final PaymentGatewayFactory paymentFactory = new PaymentGatewayFactory(PaymentGatewayFactory.CardProvider.STRIPE);
    public final OrdersService orders = new OrdersService(catalog, cart, pricing, deliveryFactory, paymentFactory,
            OrderChecks.build(SUPPORTED_REGIONS), eventBus);
    public final NotificationService notifications = new NotificationService();
    public final AnalyticsService analytics = new AnalyticsService();

    public AgroMarketApp() {
        // Observer: тыңдаушыларды тіркеу
        notifications.subscribeToEvents(eventBus);
        catalog.subscribeToEvents(eventBus);
        analytics.subscribeToEvents(eventBus);
    }
}
