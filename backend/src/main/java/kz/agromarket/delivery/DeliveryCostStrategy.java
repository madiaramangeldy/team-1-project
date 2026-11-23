package kz.agromarket.delivery;

import java.math.BigDecimal;

/**
 * STRATEGY паттерні (US-08).
 * Әр жеткізу тәсілінің құнды есептеу формуласы — бөлек класс.
 * OrdersService if/switch жазбайды, тек strategy.calculate() шақырады.
 */
public interface DeliveryCostStrategy {

    String title();

    BigDecimal calculate(double weightKg, double distanceKm);
}
