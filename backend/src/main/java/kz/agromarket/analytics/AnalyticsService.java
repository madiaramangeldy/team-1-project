package kz.agromarket.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.Events;
import kz.agromarket.common.Money;

/**
 * US-12 Сату дашборды.
 * Деректер OrderPaid оқиғасы арқылы жиналады (Observer).
 */
public class AnalyticsService {

    public record DailySales(LocalDate date, BigDecimal total) {
    }

    public record TopProduct(String productName, double quantityKg) {
    }

    public record FarmerDashboard(BigDecimal revenue, List<DailySales> weeklySales, List<TopProduct> topProducts) {
    }

    private record Sale(String farmerId, String productName, double quantityKg, BigDecimal total, LocalDate date) {
    }

    private final List<Sale> sales = new ArrayList<>();

    /** OBSERVER — тыңдаушыны тіркеу */
    public void subscribeToEvents(EventBus eventBus) {
        eventBus.subscribe(Events.OrderPaid.class, this::record);
    }

    void record(Events.OrderPaid event) {
        for (Events.PaidSubOrder sub : event.subOrders()) {
            for (Events.PaidItem item : sub.items()) {
                sales.add(new Sale(sub.farmerId(), item.productName(), item.quantityKg(), item.total(), event.paidAt()));
            }
        }
    }

    public FarmerDashboard getFarmerDashboard(String requesterId, String farmerId, LocalDate today) {
        if (!requesterId.equals(farmerId)) {
            throw new DomainException("FORBIDDEN", "Тек өз статистикаңызды көре аласыз", 403);
        }
        List<Sale> own = sales.stream().filter(s -> s.farmerId().equals(farmerId)).toList();

        List<DailySales> weekly = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            weekly.add(new DailySales(day, sum(own.stream().filter(s -> s.date().equals(day)).toList())));
        }

        Map<String, Double> byProduct = new LinkedHashMap<>();
        own.forEach(s -> byProduct.merge(s.productName(), s.quantityKg(), Double::sum));
        List<TopProduct> top = byProduct.entrySet().stream()
                .map(e -> new TopProduct(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(TopProduct::quantityKg).reversed())
                .limit(5)
                .toList();

        return new FarmerDashboard(sum(own), weekly, top);
    }

    public FarmerDashboard getFarmerDashboard(String requesterId, String farmerId) {
        return getFarmerDashboard(requesterId, farmerId, LocalDate.now());
    }

    private static BigDecimal sum(List<Sale> list) {
        return Money.round(list.stream().map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
