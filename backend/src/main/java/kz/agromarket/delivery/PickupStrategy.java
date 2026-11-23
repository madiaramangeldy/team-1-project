package kz.agromarket.delivery;

import java.math.BigDecimal;
import kz.agromarket.common.Money;

/** Өзі алып кету — тегін */
public class PickupStrategy implements DeliveryCostStrategy {

    @Override
    public String title() {
        return "Өзі алып кету";
    }

    @Override
    public BigDecimal calculate(double weightKg, double distanceKm) {
        return Money.ZERO;
    }
}
