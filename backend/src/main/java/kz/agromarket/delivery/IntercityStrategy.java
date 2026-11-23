package kz.agromarket.delivery;

import java.math.BigDecimal;
import kz.agromarket.common.Money;

/** Қалааралық жеткізу — салмақ пен қашықтық бойынша */
public class IntercityStrategy implements DeliveryCostStrategy {

    private static final double BASE_PRICE = 2000;
    private static final double PER_KG = 30;
    private static final double PER_KM = 15;

    @Override
    public String title() {
        return "Қалааралық жеткізу";
    }

    @Override
    public BigDecimal calculate(double weightKg, double distanceKm) {
        return Money.of(BASE_PRICE + weightKg * PER_KG + distanceKm * PER_KM);
    }
}
