package kz.agromarket.pricing;

import java.math.BigDecimal;
import java.util.List;
import kz.agromarket.common.Money;

/** Базалық баға: 1 кг бағасы × салмақ. Декораторлар тізбегінің ең ішкі элементі. */
public class BasePrice implements PriceComponent {

    private final BigDecimal pricePerKg;
    private final double quantityKg;

    public BasePrice(BigDecimal pricePerKg, double quantityKg) {
        this.pricePerKg = pricePerKg;
        this.quantityKg = quantityKg;
    }

    @Override
    public BigDecimal calculate() {
        return Money.round(pricePerKg.multiply(BigDecimal.valueOf(quantityKg)));
    }

    @Override
    public List<Discount> breakdown() {
        return List.of();
    }
}
