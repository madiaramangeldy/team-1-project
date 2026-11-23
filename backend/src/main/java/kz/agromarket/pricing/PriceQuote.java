package kz.agromarket.pricing;

import java.math.BigDecimal;
import java.util.List;

/** Есептелген баға: бастапқы баға, жеңілдіктер және соңғы сома */
public record PriceQuote(BigDecimal base, BigDecimal total, List<PriceComponent.Discount> discounts) {
}
