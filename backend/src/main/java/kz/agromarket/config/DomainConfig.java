package kz.agromarket.config;

import kz.agromarket.analytics.AnalyticsService;
import kz.agromarket.auth.AuthService;
import kz.agromarket.cart.CartService;
import kz.agromarket.catalog.CatalogService;
import kz.agromarket.catalog.PhotoStorage;
import kz.agromarket.common.EventBus;
import kz.agromarket.delivery.DeliveryStrategyFactory;
import kz.agromarket.notifications.NotificationService;
import kz.agromarket.orders.OrderCheck;
import kz.agromarket.orders.OrderChecks;
import kz.agromarket.orders.OrdersService;
import kz.agromarket.payments.PaymentGatewayFactory;
import kz.agromarket.pricing.PricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Домендік сервистерді Spring DI-контейнеріне тіркеу (Dependency Injection).
 *
 * Spring әр @Bean әдісін бір рет шақырады (singleton) және тәуелділіктерді
 * параметр арқылы өзі береді. Бұрын бұл жұмысты AgroMarketApp қолмен жасайтын.
 *
 * Келесі қадам: in-memory сақтауды JPA репозиторийлерімен ауыстыру
 * (әр модуль өз иесімен — Auth/Catalog/Cart — Қуаныш, Pricing/Orders — Мадияр,
 * Payments/Delivery — Димаш).
 */
@Configuration
public class DomainConfig {

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    @Bean
    public AuthService authService(EventBus eventBus) {
        return new AuthService(eventBus);
    }

    @Bean
    public PhotoStorage photoStorage() {
        return new PhotoStorage.InMemory(); // кейін MinIO
    }

    @Bean
    public CatalogService catalogService(AuthService auth, PhotoStorage storage, EventBus eventBus) {
        CatalogService catalog = new CatalogService(auth, storage);
        catalog.subscribeToEvents(eventBus); // Observer: қалдықты азайту
        return catalog;
    }

    @Bean
    public PricingService pricingService() {
        return new PricingService();
    }

    @Bean
    public CartService cartService(CatalogService catalog, PricingService pricing) {
        return new CartService(catalog, pricing);
    }

    @Bean
    public DeliveryStrategyFactory deliveryStrategyFactory() {
        return DeliveryStrategyFactory.withDefaults();
    }

    @Bean
    public PaymentGatewayFactory paymentGatewayFactory(AgroMarketProperties properties) {
        return new PaymentGatewayFactory(properties.payment().cardProvider());
    }

    @Bean
    public OrderCheck orderChecks(AgroMarketProperties properties) {
        return OrderChecks.build(properties.supportedRegions());
    }

    @Bean
    public OrdersService ordersService(CatalogService catalog, CartService cart, PricingService pricing,
                                       DeliveryStrategyFactory deliveryFactory, PaymentGatewayFactory paymentFactory,
                                       OrderCheck orderChecks, EventBus eventBus) {
        return new OrdersService(catalog, cart, pricing, deliveryFactory, paymentFactory, orderChecks, eventBus);
    }

    @Bean
    public NotificationService notificationService(EventBus eventBus) {
        NotificationService notifications = new NotificationService();
        notifications.subscribeToEvents(eventBus); // Observer
        return notifications;
    }

    @Bean
    public AnalyticsService analyticsService(EventBus eventBus) {
        AnalyticsService analytics = new AnalyticsService();
        analytics.subscribeToEvents(eventBus); // Observer
        return analytics;
    }
}
