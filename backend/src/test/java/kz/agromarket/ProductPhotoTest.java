package kz.agromarket;

import static kz.agromarket.JpaFixtures.approvedFarmer;
import static kz.agromarket.JpaFixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import kz.agromarket.auth.persistence.FarmerProfileRepository;
import kz.agromarket.catalog.application.ProductService;
import kz.agromarket.catalog.persistence.CategoryRepository;
import kz.agromarket.catalog.persistence.ProductEntity;
import kz.agromarket.catalog.persistence.ProductRepository;
import kz.agromarket.catalog.persistence.RegionRepository;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.storage.FileStorage;
import kz.agromarket.common.storage.FileUpload;
import kz.agromarket.common.storage.InMemoryFileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-03. Өнім фотосы (файл қоймасы)")
class ProductPhotoTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final FileStorage storage = new InMemoryFileStorage();
    private final ProductService service = new ProductService(products, mock(FarmerProfileRepository.class),
            mock(CategoryRepository.class), mock(RegionRepository.class), storage);

    // Фермердің user id-і = 101 (JpaFixtures: 100 + фермер id)
    private final ProductEntity tomato = product(10, approvedFarmer(1, "Айдар", "Алматы облысы"), "Қызанақ", 600, 50);

    ProductPhotoTest() {
        when(products.findById(10L)).thenReturn(Optional.of(tomato));
    }

    private static FileUpload photo(String name) {
        return new FileUpload(name, "image/jpeg", 3, new ByteArrayInputStream(new byte[] {1, 2, 3}));
    }

    @Test
    @DisplayName("US-03: фото сақталады, өнімде оны ашатын сілтеме пайда болады")
    void photoStoredAndLinked() {
        String url = service.uploadPhoto(101L, 10L, photo("tomato.jpg")).photoUrl();

        assertTrue(url.startsWith("/api/files/products/"));
        String key = url.substring("/api/files/".length());
        assertEquals(3, storage.load(key).size());
    }

    @Test
    @DisplayName("US-03: басқа фермердің өніміне фото жүктеуге болмайды — 403")
    void foreignProduct() {
        DomainException e = assertThrows(DomainException.class, () -> service.uploadPhoto(999L, 10L, photo("a.jpg")));
        assertEquals(403, e.getStatus());
    }

    @Test
    @DisplayName("US-03: фото тек JPG/PNG")
    void photoFormat() {
        assertThrows(DomainException.class, () -> service.uploadPhoto(101L, 10L, photo("tomato.gif")));
    }
}
