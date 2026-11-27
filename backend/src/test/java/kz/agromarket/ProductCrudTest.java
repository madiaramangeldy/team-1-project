package kz.agromarket;

import static kz.agromarket.JpaFixtures.approvedFarmer;
import static kz.agromarket.JpaFixtures.product;
import static kz.agromarket.JpaFixtures.user;
import static kz.agromarket.JpaFixtures.withId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.FarmerProfileRepository;
import kz.agromarket.catalog.application.ProductService;
import kz.agromarket.catalog.persistence.CategoryEntity;
import kz.agromarket.catalog.persistence.CategoryRepository;
import kz.agromarket.catalog.persistence.ProductEntity;
import kz.agromarket.catalog.persistence.ProductRepository;
import kz.agromarket.catalog.persistence.RegionEntity;
import kz.agromarket.catalog.persistence.RegionRepository;
import kz.agromarket.catalog.web.ProductDtos.ProductResponse;
import kz.agromarket.catalog.web.ProductDtos.ProductUpsertRequest;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.storage.InMemoryFileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * US-03: өнім CRUD ережелері.
 * Дерекқор жоқ: репозиторийлер Mockito-мен ауыстырылған, тек ProductService логикасы тексеріледі.
 */
@DisplayName("US-03. Өнім CRUD (ProductService)")
class ProductCrudTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final FarmerProfileRepository farmers = mock(FarmerProfileRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductService service = new ProductService(products, farmers, categories,
            mock(RegionRepository.class), new InMemoryFileStorage());

    // Верификациядан өткен фермер, оның user id-і = 101 (JpaFixtures: 100 + фермер id)
    private final FarmerProfileEntity aidar = approvedFarmer(1, "Айдар", "Алматы облысы");
    private final ProductEntity tomato = product(10, aidar, "Қызанақ", 600, 50);

    ProductCrudTest() {
        when(farmers.findByUserId(101L)).thenReturn(Optional.of(aidar));
        when(categories.findById(1L)).thenReturn(Optional.of(new CategoryEntity("Көкөніс")));
        when(products.findById(10L)).thenReturn(Optional.of(tomato));
        when(products.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static ProductUpsertRequest request(String name, int pricePerKg) {
        return new ProductUpsertRequest(name, 1L, null, BigDecimal.valueOf(pricePerKg), BigDecimal.valueOf(100),
                false, null, null, null, null);
    }

    @Test
    @DisplayName("Create: верификацияланған фермер өнім қосады, өңір фермерден алынады")
    void verifiedFarmerCreatesProduct() {
        ProductResponse created = service.create(101L, request("Картоп", 150));

        assertEquals("Картоп", created.name());
        assertEquals(BigDecimal.valueOf(150), created.pricePerKg());
        assertEquals("Алматы облысы", created.region());
    }

    @Test
    @DisplayName("Create: верификациядан өтпеген фермер өнім қоса алмайды — 403")
    void unverifiedFarmerCannotCreate() {
        FarmerProfileEntity newcomer = withId(new FarmerProfileEntity(
                user(102, "asqar@farm.kz", Role.FARMER), new RegionEntity("Астана"), "Асқар"), 2);
        when(farmers.findByUserId(102L)).thenReturn(Optional.of(newcomer));

        DomainException e = assertThrows(DomainException.class, () -> service.create(102L, request("Бал", 3000)));
        assertEquals(403, e.getStatus());
        verify(products, never()).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Read: жоқ өнім — 404")
    void missingProduct() {
        when(products.findById(999L)).thenReturn(Optional.empty());

        DomainException e = assertThrows(DomainException.class, () -> service.get(999L));
        assertEquals(404, e.getStatus());
    }

    @Test
    @DisplayName("Update: фермер өз өнімінің бағасын өзгертеді")
    void ownerUpdatesProduct() {
        ProductResponse updated = service.update(101L, 10L, request("Қызанақ", 700));

        assertEquals(BigDecimal.valueOf(700), updated.pricePerKg());
    }

    @Test
    @DisplayName("Update: басқа фермердің өнімін өзгертуге болмайды — 403")
    void foreignFarmerCannotUpdate() {
        DomainException e = assertThrows(DomainException.class, () -> service.update(999L, 10L, request("X", 1)));
        assertEquals(403, e.getStatus());
    }

    @Test
    @DisplayName("Delete: фермер өз өнімін жояды")
    void ownerDeletesProduct() {
        service.delete(101L, 10L);

        verify(products).delete(tomato);
    }

    @Test
    @DisplayName("Delete: басқа фермердің өнімін жоюға болмайды")
    void foreignFarmerCannotDelete() {
        assertThrows(DomainException.class, () -> service.delete(999L, 10L));
        verify(products, never()).delete(any(ProductEntity.class));
    }

    @Test
    @DisplayName("Validation: маусымдық жеңілдіктің соңы басынан ерте болмауы керек")
    void seasonalDatesValidated() {
        ProductUpsertRequest wrongDates = new ProductUpsertRequest("Алма", 1L, null, BigDecimal.valueOf(400),
                BigDecimal.valueOf(100), false, null, BigDecimal.TEN,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 1));

        assertThrows(DomainException.class, () -> service.create(101L, wrongDates));
    }
}
