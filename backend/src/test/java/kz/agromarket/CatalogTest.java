package kz.agromarket;

import static kz.agromarket.TestSupport.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import kz.agromarket.auth.Role;
import kz.agromarket.catalog.CatalogFilter;
import kz.agromarket.catalog.Page;
import kz.agromarket.catalog.Product;
import kz.agromarket.common.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-03, US-04. Өнімдер және каталог")
class CatalogTest {

    private final TestSupport t = new TestSupport();

    @Test
    @DisplayName("US-03: верификацияланған фермер фотосымен өнім қосады")
    void verifiedFarmerAddsProduct() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        assertEquals(t.farmerA, p.getFarmerId());
        assertTrue(p.getPhotoUrl().startsWith("minio://products/"));
    }

    @Test
    @DisplayName("US-03: верификациядан өтпеген фермер өнім қоса алмайды")
    void unverifiedFarmerCannotAdd() {
        String farmer = t.register("new", Role.FARMER);
        DomainException e = assertThrows(DomainException.class, () -> t.app.catalog.addProduct(farmer, product().build()));
        assertEquals("FARMER_NOT_VERIFIED", e.getCode());
    }

    @Test
    @DisplayName("US-03: міндетті өріс бос болса — валидация қатесі")
    void requiredFields() {
        DomainException e = assertThrows(DomainException.class,
                () -> t.app.catalog.addProduct(t.farmerA, product().name("").build()));
        assertEquals("VALIDATION", e.getCode());
    }

    @Test
    @DisplayName("US-03: фермер тек өз өнімін өзгерте алады")
    void onlyOwnerCanUpdate() {
        Product p = t.app.catalog.addProduct(t.farmerA, product().build());
        DomainException e = assertThrows(DomainException.class,
                () -> t.app.catalog.updateProduct(t.farmerB, p.getId(), product().pricePerKg(1).build()));
        assertEquals(403, e.getStatus());
    }

    private void seed() {
        t.app.catalog.addProduct(t.farmerA, product().name("Қызанақ").organic(true).pricePerKg(600).build());
        t.app.catalog.addProduct(t.farmerA, product().name("Алма").category("Жеміс").organic(false).pricePerKg(450).build());
        t.app.catalog.addProduct(t.farmerB, product().name("Картоп").region("Астана").organic(false).pricePerKg(250).build());
    }

    private List<String> names(CatalogFilter filter) {
        return t.app.catalog.search(filter).items().stream().map(Product::getName).toList();
    }

    @Test
    @DisplayName("US-04: сүзгілер бірге қолданылады")
    void combinedFilters() {
        seed();
        assertEquals(List.of("Қызанақ"),
                names(new CatalogFilter().category("Көкөніс").region("Алматы облысы").organic(true)));
    }

    @Test
    @DisplayName("US-04: баға аралығы бойынша сүзу")
    void priceRange() {
        seed();
        assertEquals(List.of("Алма"), names(new CatalogFilter().minPrice(300).maxPrice(500)));
    }

    @Test
    @DisplayName("US-04: пагинация — бір бетте 20 өнім")
    void pagination() {
        for (int i = 0; i < 25; i++) {
            t.app.catalog.addProduct(t.farmerA, product().name("Өнім " + i).build());
        }
        assertEquals(20, t.app.catalog.search(new CatalogFilter().page(1)).items().size());
        assertEquals(5, t.app.catalog.search(new CatalogFilter().page(2)).items().size());
    }

    @Test
    @DisplayName("US-04: нәтиже болмаса — бос тізім, қате емес")
    void emptyResult() {
        seed();
        Page<Product> page = t.app.catalog.search(new CatalogFilter().category("Ет"));
        assertTrue(page.items().isEmpty());
        assertEquals(0, page.total());
    }
}
