package kz.agromarket.catalog.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import kz.agromarket.catalog.persistence.ProductEntity;

/** Catalog API-дің DTO-лары. */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** POST /api/products және PUT /api/products/{id} */
    public record ProductUpsertRequest(
            @NotBlank(message = "Атауы міндетті") @Size(max = 150) String name,
            @NotNull(message = "Санат міндетті") Long categoryId,
            /* болмаса, фермердің өңірі алынады */ Long regionId,
            @NotNull(message = "1 кг бағасы міндетті")
            @Positive(message = "Баға 0-ден үлкен болуы керек") BigDecimal pricePerKg,
            @NotNull(message = "Қалдық міндетті")
            @PositiveOrZero(message = "Қалдық теріс бола алмайды") BigDecimal stockKg,
            boolean organic,
            @DecimalMin("0") @DecimalMax("100") BigDecimal wholesaleDiscountPercent,
            @DecimalMin("0") @DecimalMax("100") BigDecimal seasonalDiscountPercent,
            LocalDate seasonalFrom,
            LocalDate seasonalTo) {
    }

    /** Өнім туралы жауап */
    public record ProductResponse(
            Long id,
            String name,
            String category,
            String region,
            BigDecimal pricePerKg,
            BigDecimal stockKg,
            boolean organic,
            String photoUrl,
            Long farmerId,
            String farmName,
            BigDecimal farmerRating,
            BigDecimal wholesaleDiscountPercent,
            BigDecimal seasonalDiscountPercent,
            LocalDate seasonalFrom,
            LocalDate seasonalTo,
            Long categoryId,
            Long regionId) {

        public static ProductResponse of(ProductEntity p) {
            return new ProductResponse(p.getId(), p.getName(), p.getCategory().getName(), p.getRegion().getName(),
                    p.getPricePerKg(), p.getStockKg(), p.isOrganic(), p.getPhotoUrl(),
                    p.getFarmer().getId(), p.getFarmer().getFarmName(), p.getFarmer().getRating(),
                    p.getWholesaleDiscountPercent(), p.getSeasonalDiscountPercent(),
                    p.getSeasonalFrom(), p.getSeasonalTo(),
                    p.getCategory().getId(), p.getRegion().getId());
        }
    }

    /** Анықтамалық элементі (өңір, санат) */
    public record Option(Long id, String name) {
    }
}
