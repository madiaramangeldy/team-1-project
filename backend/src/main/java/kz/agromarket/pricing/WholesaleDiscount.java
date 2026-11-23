package kz.agromarket.pricing;

import java.math.BigDecimal;
import kz.agromarket.common.Money;

/** US-06: 50 кг-нан бастап көтерме жеңілдік */
public class WholesaleDiscount extends PriceDecorator {

    public static final double THRESHOLD_KG = 50;

    private final double quantityKg;
    private final double percent;

    public WholesaleDiscount(PriceComponent inner, double quantityKg, double percent) {
        super(inner);
        this.quantityKg = quantityKg;
        this.percent = percent;
    }

    @Override
    protected String label() {
        return "Көтерме жеңілдік " + (int) percent + "% (" + (int) THRESHOLD_KG + " кг-нан бастап)";
    }

    @Override
    protected BigDecimal apply(BigDecimal price) {
        return quantityKg >= THRESHOLD_KG ? Money.minusPercent(price, percent) : price;
    }
}
