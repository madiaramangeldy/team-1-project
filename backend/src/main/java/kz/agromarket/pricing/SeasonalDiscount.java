package kz.agromarket.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import kz.agromarket.catalog.SeasonalOffer;
import kz.agromarket.common.Money;

/** US-07: белгілі мерзімдегі маусымдық жеңілдік */
public class SeasonalDiscount extends PriceDecorator {

    private final SeasonalOffer offer;
    private final LocalDate today;

    public SeasonalDiscount(PriceComponent inner, SeasonalOffer offer, LocalDate today) {
        super(inner);
        this.offer = offer;
        this.today = today;
    }

    @Override
    protected String label() {
        return "Маусымдық жеңілдік " + (int) offer.percent() + "%";
    }

    @Override
    protected BigDecimal apply(BigDecimal price) {
        return offer.isActive(today) ? Money.minusPercent(price, offer.percent()) : price;
    }
}
