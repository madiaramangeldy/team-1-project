package kz.agromarket.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ақшамен жұмыс. double емес, BigDecimal қолданылады:
 * double дөңгелектеу қателерін береді (0.1 + 0.2 = 0.30000000000000004).
 */
public final class Money {

    public static final BigDecimal ZERO = of(0);

    private Money() {
    }

    public static BigDecimal of(double value) {
        return round(BigDecimal.valueOf(value));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Пайыздық жеңілдікті қолдану: price × (100 − percent) / 100 */
    public static BigDecimal minusPercent(BigDecimal price, double percent) {
        BigDecimal factor = BigDecimal.valueOf(100 - percent).divide(BigDecimal.valueOf(100));
        return round(price.multiply(factor));
    }
}
