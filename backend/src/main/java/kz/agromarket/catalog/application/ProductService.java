package kz.agromarket.catalog.application;

import java.util.List;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.FarmerProfileRepository;
import kz.agromarket.catalog.persistence.CategoryEntity;
import kz.agromarket.catalog.persistence.CategoryRepository;
import kz.agromarket.catalog.persistence.ProductEntity;
import kz.agromarket.catalog.persistence.ProductRepository;
import kz.agromarket.catalog.persistence.ProductSpecifications;
import kz.agromarket.catalog.persistence.RegionEntity;
import kz.agromarket.catalog.persistence.RegionRepository;
import kz.agromarket.catalog.web.ProductDtos.ProductResponse;
import kz.agromarket.catalog.web.ProductDtos.ProductUpsertRequest;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.storage.FileStorage;
import kz.agromarket.common.storage.FileUpload;
import kz.agromarket.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-03 Өнім CRUD, US-04 Каталог сүзгісі мен пагинациясы — дерекқормен жұмыс.
 *
 * Ережелер:
 *  - өнімді тек верификацияланған фермер қоса алады;
 *  - фермер тек өз өнімін өзгерте және жоя алады;
 *  - каталогта тек верификацияланған фермерлердің өнімдері көрінеді.
 */
@Service
@Transactional
public class ProductService {

    public static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository products;
    private final FarmerProfileRepository farmers;
    private final CategoryRepository categories;
    private final RegionRepository regions;
    private final FileStorage storage;

    public ProductService(ProductRepository products, FarmerProfileRepository farmers,
                          CategoryRepository categories, RegionRepository regions, FileStorage storage) {
        this.products = products;
        this.farmers = farmers;
        this.categories = categories;
        this.regions = regions;
        this.storage = storage;
    }

    // ---------- US-04: каталог ----------

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSpecifications.Filter filter, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(safePage - 1, safeSize, Sort.by("id"));
        var spec = ProductSpecifications.catalog(filter);
        return PageResponse.of(products.findAll(spec, pageable).map(ProductResponse::of));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.of(product(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> mine(Long userId) {
        return products.findByFarmerUserIdOrderByIdDesc(userId).stream().map(ProductResponse::of).toList();
    }

    // ---------- US-03: CRUD ----------

    public ProductResponse create(Long userId, ProductUpsertRequest request) {
        FarmerProfileEntity farmer = verifiedFarmer(userId);
        RegionEntity region = request.regionId() == null ? farmer.getRegion() : region(request.regionId());

        ProductEntity product = new ProductEntity(farmer, category(request.categoryId()), region,
                request.name(), request.pricePerKg(), request.stockKg(), request.organic());
        applyDiscounts(product, request);
        return ProductResponse.of(products.save(product));
    }

    public ProductResponse update(Long userId, Long productId, ProductUpsertRequest request) {
        ProductEntity product = ownProduct(userId, productId);
        RegionEntity region = request.regionId() == null ? product.getRegion() : region(request.regionId());

        product.update(category(request.categoryId()), region, request.name(),
                request.pricePerKg(), request.stockKg(), request.organic());
        applyDiscounts(product, request);
        return ProductResponse.of(product);
    }

    public void delete(Long userId, Long productId) {
        products.delete(ownProduct(userId, productId));
    }

    /** US-03: өнім фотосы (JPG/PNG, 5 МБ-қа дейін). Файл MinIO-да, дерекқорда тек сілтеме */
    public ProductResponse uploadPhoto(Long userId, Long productId, FileUpload file) {
        ProductEntity product = ownProduct(userId, productId);
        file.validate(FileUpload.PHOTO_EXTENSIONS, "Фото JPG немесе PNG форматында болуы керек");
        String key = storage.store("products", file.extension(), file.contentType(), file.data(), file.size());
        product.setPhotoUrl("/api/files/" + key);
        return ProductResponse.of(product);
    }

    // ---------- Көмекші әдістер ----------

    private FarmerProfileEntity verifiedFarmer(Long userId) {
        FarmerProfileEntity farmer = farmers.findByUserId(userId)
                .orElseThrow(() -> new DomainException("FORBIDDEN", "Фермер профилі табылмады", 403));
        if (!farmer.isVerified()) {
            throw new DomainException("FARMER_NOT_VERIFIED", "Өнім қосу үшін верификациядан өту керек", 403);
        }
        return farmer;
    }

    private ProductEntity ownProduct(Long userId, Long productId) {
        ProductEntity product = product(productId);
        if (!product.getFarmer().getUser().getId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "Тек өз өніміңізді өзгерте аласыз", 403);
        }
        return product;
    }

    private void applyDiscounts(ProductEntity product, ProductUpsertRequest request) {
        if (request.seasonalFrom() != null && request.seasonalTo() != null
                && request.seasonalTo().isBefore(request.seasonalFrom())) {
            throw new DomainException("VALIDATION", "Маусымдық жеңілдіктің соңы басынан кейін болуы керек");
        }
        product.setDiscounts(request.wholesaleDiscountPercent(), request.seasonalDiscountPercent(),
                request.seasonalFrom(), request.seasonalTo());
    }

    private ProductEntity product(Long id) {
        return products.findById(id).orElseThrow(() -> new DomainException("NOT_FOUND", "Өнім табылмады", 404));
    }

    private CategoryEntity category(Long id) {
        return categories.findById(id).orElseThrow(() -> new DomainException("NOT_FOUND", "Санат табылмады", 404));
    }

    private RegionEntity region(Long id) {
        return regions.findById(id).orElseThrow(() -> new DomainException("NOT_FOUND", "Өңір табылмады", 404));
    }
}
