package kz.agromarket.pricing;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import kz.agromarket.catalog.Product;
import kz.agromarket.common.DomainException;

/**
 * US-06, US-07. Декораторлар тізбегін құрастырады:
 * BasePrice → WholesaleDiscount → SeasonalDiscount → PromoCodeDiscount
 */
public class PricingService {

    private final Map<String, PromoCode> promoCodes = new HashMap<>();

    public void addPromoCode(PromoCode promo) {
        promoCodes.put(promo.code().toUpperCase(), promo);
    }

    public PriceQuote quote(Product product, double quantityKg, String promoCode, LocalDate today) {
        BasePrice base = new BasePrice(product.getPricePerKg(), quantityKg);
        PriceComponent price = base;

        if (product.getWholesaleDiscountPercent() > 0) {
            price = new WholesaleDiscount(price, quantityKg, product.getWholesaleDiscountPercent());
        }
        if (product.getSeasonalOffer() != null) {
            price = new SeasonalDiscount(price, product.getSeasonalOffer(), today);
        }
        if (promoCode != null) {
            price = new PromoCodeDiscount(price, validatePromo(promoCode, today));
        }

        return new PriceQuote(base.calculate(), price.calculate(), price.breakdown());
    }

    public PriceQuote quote(Product product, double quantityKg) {
        return quote(product, quantityKg, null, LocalDate.now());
    }

    public PromoCode validatePromo(String code, LocalDate today) {
        PromoCode promo = promoCodes.get(code.toUpperCase());
        if (promo == null) {
            throw new DomainException("PROMO_INVALID", "«" + code + "» промокоды табылмады");
        }
        if (today.isAfter(promo.validUntil())) {
            throw new DomainException("PROMO_EXPIRED", "«" + code + "» промокодының мерзімі өтіп кеткен");
        }
        return promo;
    }
}
