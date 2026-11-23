package kz.agromarket.cart;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kz.agromarket.catalog.CatalogService;
import kz.agromarket.catalog.Product;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.Money;
import kz.agromarket.pricing.PriceQuote;
import kz.agromarket.pricing.PricingService;

/**
 * US-05 Себет.
 * Тек себетті басқарады; бағаны PricingService есептейді (SOLID — S).
 */
public class CartService {

    public record CartLine(String productId, String productName, String farmerId, double quantityKg, PriceQuote price) {
    }

    public record CartView(List<CartLine> lines, double totalWeightKg, BigDecimal total) {
    }

    /** Себет сатып алушының ID-і бойынша сақталады, сондықтан қайта кіргенде жоғалмайды */
    private final Map<String, Map<String, Double>> carts = new HashMap<>();
    private final CatalogService catalog;
    private final PricingService pricing;

    public CartService(CatalogService catalog, PricingService pricing) {
        this.catalog = catalog;
        this.pricing = pricing;
    }

    public CartView add(String buyerId, String productId, double quantityKg) {
        assertPositive(quantityKg);
        catalog.getProduct(productId); // өнім бар екенін тексеру
        items(buyerId).merge(productId, quantityKg, Double::sum);
        return view(buyerId);
    }

    public CartView updateQuantity(String buyerId, String productId, double quantityKg) {
        assertPositive(quantityKg);
        if (!items(buyerId).containsKey(productId)) {
            throw new DomainException("NOT_FOUND", "Бұл өнім себетте жоқ", 404);
        }
        items(buyerId).put(productId, quantityKg);
        return view(buyerId);
    }

    public CartView remove(String buyerId, String productId) {
        items(buyerId).remove(productId);
        return view(buyerId);
    }

    public CartView view(String buyerId) {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<String, Double> item : items(buyerId).entrySet()) {
            Product product = catalog.getProduct(item.getKey());
            PriceQuote price = pricing.quote(product, item.getValue(), null, LocalDate.now());
            lines.add(new CartLine(product.getId(), product.getName(), product.getFarmerId(), item.getValue(), price));
        }
        double weight = lines.stream().mapToDouble(CartLine::quantityKg).sum();
        BigDecimal total = Money.round(lines.stream().map(l -> l.price().total()).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new CartView(lines, weight, total);
    }

    /** productId → салмақ (кг) */
    public Map<String, Double> items(String buyerId) {
        return carts.computeIfAbsent(buyerId, k -> new LinkedHashMap<>());
    }

    public void clear(String buyerId) {
        items(buyerId).clear();
    }

    private static void assertPositive(double quantityKg) {
        if (!(quantityKg > 0)) {
            throw new DomainException("VALIDATION", "Салмақ 0-ден үлкен болуы керек");
        }
    }
}
