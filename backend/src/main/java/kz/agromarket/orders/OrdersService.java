package kz.agromarket.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kz.agromarket.cart.CartService;
import kz.agromarket.catalog.CatalogService;
import kz.agromarket.catalog.Product;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.Events;
import kz.agromarket.common.Ids;
import kz.agromarket.common.Money;
import kz.agromarket.delivery.DeliveryCostStrategy;
import kz.agromarket.delivery.DeliveryStrategyFactory;
import kz.agromarket.payments.PaymentGateway;
import kz.agromarket.payments.PaymentGatewayFactory;
import kz.agromarket.pricing.PriceQuote;
import kz.agromarket.pricing.PricingService;

/**
 * US-09 Тапсырыс рәсімдеу, US-10 Төлем, US-11 Төлемнен кейінгі оқиға.
 *
 * Назар аударыңыз: бұл сервисте нақты жеңілдік, жеткізу немесе төлем класы жоқ.
 * Бәрі интерфейс және фабрика арқылы келеді (SOLID — D).
 */
public class OrdersService {

    private final Map<String, Order> orders = new HashMap<>();
    private final CatalogService catalog;
    private final CartService cart;
    private final PricingService pricing;
    private final DeliveryStrategyFactory deliveryFactory;
    private final PaymentGatewayFactory paymentFactory;
    private final OrderCheck checks;
    private final EventBus eventBus;

    public OrdersService(CatalogService catalog, CartService cart, PricingService pricing,
                         DeliveryStrategyFactory deliveryFactory, PaymentGatewayFactory paymentFactory,
                         OrderCheck checks, EventBus eventBus) {
        this.catalog = catalog;
        this.cart = cart;
        this.pricing = pricing;
        this.deliveryFactory = deliveryFactory;
        this.paymentFactory = paymentFactory;
        this.checks = checks;
        this.eventBus = eventBus;
    }

    // ---------- US-09 ----------

    public Order placeOrder(String buyerId, PlaceOrderRequest request, LocalDate today) {
        Map<String, Double> cartItems = cart.items(buyerId);
        if (cartItems.isEmpty()) {
            throw new DomainException("CART_EMPTY", "Себет бос");
        }

        List<OrderDraft.Line> lines = new ArrayList<>();
        cartItems.forEach((productId, kg) -> lines.add(new OrderDraft.Line(catalog.getProduct(productId), kg)));
        OrderDraft draft = new OrderDraft(buyerId, request.buyerRegion(), request.deliveryMethod(), lines);

        // Chain of Responsibility: қате болса, осы жерде тоқтайды
        checks.handle(draft);

        // Ішкі тапсырыстарға бөлу: әр фермерге бөлек
        Map<String, List<Order.Item>> itemsByFarmer = new LinkedHashMap<>();
        for (OrderDraft.Line line : lines) {
            Product product = line.product();
            PriceQuote price = pricing.quote(product, line.quantityKg(), request.promoCode(), today); // Decorator
            itemsByFarmer.computeIfAbsent(product.getFarmerId(), k -> new ArrayList<>())
                    .add(new Order.Item(product.getId(), product.getName(), line.quantityKg(), price));
        }
        List<Order.SubOrder> subOrders = new ArrayList<>();
        itemsByFarmer.forEach((farmerId, items) -> subOrders.add(
                new Order.SubOrder(Ids.next("sub"), farmerId, items, sum(items))));

        // Strategy + Factory Method: жеткізу құны
        DeliveryCostStrategy delivery = deliveryFactory.create(request.deliveryMethod());
        BigDecimal deliveryCost = delivery.calculate(draft.totalWeightKg(), request.distanceKm());

        BigDecimal itemsTotal = Money.round(subOrders.stream()
                .map(Order.SubOrder::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        Order order = new Order(Ids.next("order"), buyerId, subOrders, request.deliveryMethod(),
                deliveryCost, request.paymentMethod(), itemsTotal);

        orders.put(order.getId(), order);
        cart.clear(buyerId);
        return order;
    }

    public Order placeOrder(String buyerId, PlaceOrderRequest request) {
        return placeOrder(buyerId, request, LocalDate.now());
    }

    // ---------- US-10 ----------

    public Order pay(String orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != Order.Status.CREATED && order.getStatus() != Order.Status.PAYMENT_FAILED) {
            throw new DomainException("ORDER_ALREADY_PROCESSED", "Бұл тапсырыс бұрын төленген");
        }

        // Factory Method + Adapter: қай провайдер екенін сервис білмейді
        PaymentGateway gateway = paymentFactory.create(order.getPaymentMethod());
        PaymentGateway.PaymentResult result = gateway.pay(
                new PaymentGateway.PaymentRequest(order.getId(), order.getTotal(), "KZT"));

        order.setTransactionId(result.transactionId());
        if (!result.success()) {
            order.setStatus(Order.Status.PAYMENT_FAILED);
        } else if (result.status() == PaymentGateway.Status.PENDING) {
            order.setStatus(Order.Status.AWAITING_CASH);
        } else {
            markPaid(order);
        }
        return order;
    }

    /** Курьер қолма-қол ақшаны алғаннан кейін растайды */
    public Order confirmCashPayment(String orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != Order.Status.AWAITING_CASH) {
            throw new DomainException("INVALID_STATUS", "Тапсырыс қолма-қол төлемді күтіп тұрған жоқ");
        }
        markPaid(order);
        return order;
    }

    public Order getOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new DomainException("NOT_FOUND", "Тапсырыс табылмады", 404);
        }
        return order;
    }

    // ---------- US-11 ----------

    private void markPaid(Order order) {
        order.setStatus(Order.Status.PAID);

        // Observer: хабарлама, қалдық, статистиканы тыңдаушылар өздері орындайды
        List<Events.PaidSubOrder> paid = order.getSubOrders().stream()
                .map(s -> new Events.PaidSubOrder(s.farmerId(), s.total(), s.items().stream()
                        .map(i -> new Events.PaidItem(i.productId(), i.productName(), i.quantityKg(), i.price().total()))
                        .toList()))
                .toList();
        eventBus.publish(new Events.OrderPaid(order.getId(), order.getBuyerId(), LocalDate.now(), paid));
    }

    private static BigDecimal sum(List<Order.Item> items) {
        return Money.round(items.stream().map(i -> i.price().total()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
