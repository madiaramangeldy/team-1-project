package kz.agromarket.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Барлық жеңілдік-декораторлардың негізі.
 * Ішкі бағаны (inner) алып, оған өз жеңілдігін қолданады.
 */
public abstract class PriceDecorator implements PriceComponent {

    protected final PriceComponent inner;

    protected PriceDecorator(PriceComponent inner) {
        this.inner = inner;
    }

    /** Жеңілдік атауы (чекте көрсетіледі) */
    protected abstract String label();

    /** Ішкі бағаға жеңілдікті қолдану */
    protected abstract BigDecimal apply(BigDecimal price);

    @Override
    public BigDecimal calculate() {
        return apply(inner.calculate());
    }

    @Override
    public List<Discount> breakdown() {
        List<Discount> result = new ArrayList<>(inner.breakdown());
        BigDecimal discount = inner.calculate().subtract(calculate());
        if (discount.signum() > 0) {
            result.add(new Discount(label(), discount));
        }
        return result;
    }
}
