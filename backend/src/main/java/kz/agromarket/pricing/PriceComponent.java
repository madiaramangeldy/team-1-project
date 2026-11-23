package kz.agromarket.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * DECORATOR паттерні (US-06, US-07) — ортақ интерфейс.
 *
 * Әр жеңілдік — бағаны «орайтын» бөлек класс:
 *   new PromoCodeDiscount(new SeasonalDiscount(new WholesaleDiscount(new BasePrice(...))))
 *
 * Неге мұрагерлік емес: жеңілдіктердің әр комбинациясына бөлек класс керек болар еді
 * (WholesaleSeasonalPromoPrice т.б.). Декоратормен жаңа жеңілдік = бір жаңа класс (SOLID — O).
 */
public interface PriceComponent {

    BigDecimal calculate();

    /** Сатып алушыға көрсетілетін жеңілдіктер тізімі */
    List<Discount> breakdown();

    record Discount(String label, BigDecimal amount) {
    }
}
