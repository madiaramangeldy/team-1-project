package kz.agromarket.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kz.agromarket.catalog.persistence.CategoryRepository;
import kz.agromarket.catalog.persistence.RegionRepository;
import kz.agromarket.catalog.web.ProductDtos.Option;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Анықтамалықтар: frontend-тегі сүзгілер мен формалар үшін (V2 seed миграциясы толтырады). */
@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Catalog", description = "Өңірлер мен санаттар")
@Transactional(readOnly = true)
public class CatalogController {

    private final RegionRepository regions;
    private final CategoryRepository categories;

    public CatalogController(RegionRepository regions, CategoryRepository categories) {
        this.regions = regions;
        this.categories = categories;
    }

    @GetMapping("/regions")
    @Operation(summary = "Өңірлер тізімі")
    public List<Option> regions() {
        return regions.findAll(Sort.by("id")).stream().map(r -> new Option(r.getId(), r.getName())).toList();
    }

    @GetMapping("/categories")
    @Operation(summary = "Санаттар тізімі")
    public List<Option> categories() {
        return categories.findAll(Sort.by("id")).stream().map(c -> new Option(c.getId(), c.getName())).toList();
    }
}
