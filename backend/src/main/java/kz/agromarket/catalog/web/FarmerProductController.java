package kz.agromarket.catalog.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kz.agromarket.catalog.application.ProductService;
import kz.agromarket.catalog.web.ProductDtos.ProductResponse;
import kz.agromarket.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Фермер кабинеті: өз өнімдерінің тізімі. Тек FARMER рөлі. */
@RestController
@RequestMapping("/api/farmer/products")
@Tag(name = "Farmer", description = "Фермер кабинеті")
@SecurityRequirement(name = "bearerAuth")
public class FarmerProductController {

    private final ProductService products;

    public FarmerProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    @Operation(summary = "Менің өнімдерім")
    public List<ProductResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return products.mine(user.id());
    }
}
