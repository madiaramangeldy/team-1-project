package kz.agromarket;

import static kz.agromarket.TestSupport.assertMoney;
import static kz.agromarket.TestSupport.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import kz.agromarket.cart.CartService;
import kz.agromarket.catalog.Product;
import kz.agromarket.catalog.SeasonalOffer;
import kz.agromarket.common.DomainException;
import kz.agromarket.pricing.BasePrice;
import kz.agromarket.pricing.PriceComponent;
import kz.agromarket.pricing.PriceQuote;
import kz.agromarket.pricing.PromoCode;
import kz.agromarket.pricing.PromoCodeDiscount;
import kz.agromarket.pricing.SeasonalDiscount;
import kz.agromarket.pricing.WholesaleDiscount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-05 – US-07. Себет және баға (Decorator)")
class CartAndPricingTest {

    private final TestSupport t = new TestSupport();
    private static final BigDecimal P100 = BigDecimal.valueOf(100);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static final SeasonalOffer AUTUMN = new SeasonalOffer(10, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31));

    // ---------- US-05 ----------

    @Test
    @DisplayName("US-05: әртүрлі фермерлердің өнімдері бір себетке салынады")
    void cartWithTwoFarmers() {
        Product a = t.app.catalog.addProduct(t.farmerA, product().pricePerKg(100).build());
        Product b = t.app.catalog.addProduct(t.farmerB, product().pricePerKg(200).build());

        t.app.cart.add(t.buyer, a.getId(), 5);
        CartService.CartView view = t.app.cart.add(t.buyer, b.getId(), 2);

        assertEquals(2, view.lines().stream().map(CartService.CartLine::farmerId).distinct().count());
        assertMoney(5 * 100 + 2 * 200, view.total());
    }

    @Test
    @DisplayName("US-05: салмақты өзгертуге және өнімді алып тастауға болады")
    void updateAndRemove() {
        Product a = t.app.catalog.addProduct(t.farmerA, product().pricePerKg(100).build());
        t.app.cart.add(t.buyer, a.getId(), 5);
        assertMoney(800, t.app.cart.updateQuantity(t.buyer, a.getId(), 8).total());
        assertTrue(t.app.cart.remove(t.buyer, a.getId()).lines().isEmpty());
    }

    @Test
    @DisplayName("US-05: салмақ 0 немесе теріс болса — қате")
    void zeroQuantityRejected() {
        Product a = t.app.catalog.addProduct(t.farmerA, product().build());
        assertThrows(DomainException.class, () -> t.app.cart.add(t.buyer, a.getId(), 0));
    }

    // ---------- US-06 ----------

    @Test
    @DisplayName("US-06: 50 кг және одан көп болса, көтерме жеңілдік қолданылады")
    void wholesaleApplied() {
        PriceComponent price = new WholesaleDiscount(new BasePrice(P100, 50), 50, 10);
        assertMoney(4500, price.calculate());
    }

    @Test
    @DisplayName("US-06: 50 кг-нан аз болса, бөлшек баға қалады")
    void wholesaleNotApplied() {
        PriceComponent price = new WholesaleDiscount(new BasePrice(P100, 49), 49, 10);
        assertMoney(4900, price.calculate());
        assertTrue(price.breakdown().isEmpty());
    }

    @Test
    @DisplayName("US-06: бастапқы баға мен жеңілдік бөлек көрсетіледі")
    void discountShownSeparately() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().pricePerKg(100).wholesaleDiscount(10).build());
        PriceQuote quote = t.app.pricing.quote(p, 60);
        assertMoney(6000, quote.base());
        assertMoney(5400, quote.total());
        assertMoney(600, quote.discounts().get(0).amount());
    }

    // ---------- US-07 ----------

    @Test
    @DisplayName("US-07: декораторлар тізбегі — көтерме → маусымдық → промокод")
    void decoratorChain() {
        PriceComponent price = new BasePrice(P100, 100);                       // 10 000
        price = new WholesaleDiscount(price, 100, 10);                         //  9 000
        price = new SeasonalDiscount(price, AUTUMN, TODAY);                    //  8 100
        price = new PromoCodeDiscount(price, new PromoCode("AGRO10", 10, TODAY)); // 7 290
        assertMoney(7290, price.calculate());
        assertEquals(3, price.breakdown().size());
    }

    @Test
    @DisplayName("US-07: маусымдық жеңілдік мерзімнен тыс қолданылмайды")
    void seasonalOutsidePeriod() {
        PriceComponent price = new SeasonalDiscount(new BasePrice(P100, 10), AUTUMN, LocalDate.of(2026, 1, 1));
        assertMoney(1000, price.calculate());
    }

    @Test
    @DisplayName("US-07: мерзімі өткен немесе жоқ промокод түсінікті қате қайтарады")
    void invalidPromo() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        t.app.pricing.addPromoCode(new PromoCode("OLD", 5, LocalDate.of(2020, 1, 1)));

        DomainException expired = assertThrows(DomainException.class, () -> t.app.pricing.quote(p, 10, "OLD", TODAY));
        DomainException missing = assertThrows(DomainException.class, () -> t.app.pricing.quote(p, 10, "NOPE", TODAY));
        assertEquals("PROMO_EXPIRED", expired.getCode());
        assertEquals("PROMO_INVALID", missing.getCode());
    }
}
