package kz.agromarket.orders;

import java.util.List;
import kz.agromarket.catalog.Product;
import kz.agromarket.delivery.DeliveryMethod;

/** Тексерулер тізбегіне берілетін тапсырыс жобасы (әлі сақталмаған) */
public record OrderDraft(String buyerId, String buyerRegion, DeliveryMethod deliveryMethod, List<Line> lines) {

    public record Line(Product product, double quantityKg) {
    }

    public double totalWeightKg() {
        return lines.stream().mapToDouble(Line::quantityKg).sum();
    }
}
