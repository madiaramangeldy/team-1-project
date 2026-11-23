package kz.agromarket.delivery;

import java.math.BigDecimal;
import kz.agromarket.common.Money;

/** Қала ішіндегі курьер — тұрақты баға */
public class CityCourierStrategy implements DeliveryCostStrategy {

    private static final double FIXED_PRICE = 1500;

    @Override
    public String title() {
        return "Қала ішіндегі курьер";
    }

    @Override
    public BigDecimal calculate(double weightKg, double distanceKm) {
        return Money.of(FIXED_PRICE);
    }
}
