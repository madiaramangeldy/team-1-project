package kz.agromarket.pricing;

import java.math.BigDecimal;
import kz.agromarket.common.Money;

/** US-07: промокод бойынша жеңілдік */
public class PromoCodeDiscount extends PriceDecorator {

    private final PromoCode promo;

    public PromoCodeDiscount(PriceComponent inner, PromoCode promo) {
        super(inner);
        this.promo = promo;
    }

    @Override
    protected String label() {
        return "Промокод " + promo.code() + " (" + (int) promo.percent() + "%)";
    }

    @Override
    protected BigDecimal apply(BigDecimal price) {
        return Money.minusPercent(price, promo.percent());
    }
}
