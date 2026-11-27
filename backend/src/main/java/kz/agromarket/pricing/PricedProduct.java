package kz.agromarket.pricing;

import java.math.BigDecimal;
import kz.agromarket.catalog.SeasonalOffer;

/**
 * Бағасы есептелетін өнім — паттерндер қолданатын ортақ «келісім-шарт».
 *
 * Оны екі класс жүзеге асырады:
 *  - catalog.Product           — жадтағы (in-memory) домендік модель, unit-тесттер үшін;
 *  - catalog.persistence.ProductEntity — PostgreSQL-дағы нақты өнім (JPA).
 *
 * Сондықтан Decorator (баға) мен Chain of Responsibility (тапсырыс тексерулері)
 * бір рет жазылған және екі жерде де өзгеріссіз жұмыс істейді (SOLID — D: абстракцияға тәуелділік).
 */
public interface PricedProduct {

    String getName();

    BigDecimal getPricePerKg();

    /** Қоймадағы қалдық, кг */
    double availableKg();

    /** US-06: көтерме жеңілдік, % (жоқ болса 0) */
    double wholesalePercent();

    /** US-07: маусымдық ұсыныс (жоқ болса null) */
    SeasonalOffer seasonalOffer();
}
