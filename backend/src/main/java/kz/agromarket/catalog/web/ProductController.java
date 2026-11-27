package kz.agromarket.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import kz.agromarket.catalog.application.ProductService;
import kz.agromarket.catalog.persistence.ProductSpecifications;
import kz.agromarket.catalog.web.ProductDtos.ProductResponse;
import kz.agromarket.catalog.web.ProductDtos.ProductUpsertRequest;
import kz.agromarket.common.web.Files;
import kz.agromarket.common.web.PageResponse;
import kz.agromarket.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * US-03 Өнім CRUD, US-04 Каталог.
 * GET — барлығына ашық; POST, PUT, DELETE — тек FARMER (SecurityConfig).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Өнімдер каталогы және CRUD (US-03, US-04)")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    @Operation(summary = "Каталог: сүзгілер, атауы бойынша іздеу (q), пагинация (бет 1-ден басталады)")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean organic,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long farmerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new ProductSpecifications.Filter(category, region, organic, minPrice, maxPrice, q, farmerId);
        return products.search(filter, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Бір өнім")
    public ProductResponse get(@PathVariable Long id) {
        return products.get(id);
    }

    @PostMapping
    @Operation(summary = "Өнім қосу (тек верификацияланған FARMER)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> create(@AuthenticationPrincipal AuthUser user,
                                                  @Valid @RequestBody ProductUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(products.create(user.id(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Өз өнімін өзгерту (FARMER)")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                  @Valid @RequestBody ProductUpsertRequest request) {
        return products.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Өз өнімін жою (FARMER)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        products.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Өнім фотосын жүктеу (JPG/PNG, 5 МБ-қа дейін, FARMER)")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse uploadPhoto(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                       @RequestParam("file") MultipartFile file) {
        return products.uploadPhoto(user.id(), id, Files.upload(file));
    }
}
