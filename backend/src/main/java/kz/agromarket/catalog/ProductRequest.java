package kz.agromarket.catalog;

import java.math.BigDecimal;
import kz.agromarket.common.UploadedFile;

/**
 * Өнім қосу сұрауы (DTO). Өрістер көп болғандықтан, Builder арқылы жасалады.
 */
public record ProductRequest(
        String name,
        String category,
        String region,
        BigDecimal pricePerKg,
        double stockKg,
        boolean organic,
        UploadedFile photo,
        double wholesaleDiscountPercent,
        SeasonalOffer seasonalOffer) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String category;
        private String region;
        private BigDecimal pricePerKg;
        private double stockKg;
        private boolean organic;
        private UploadedFile photo = new UploadedFile("photo.jpg", 100_000);
        private double wholesaleDiscountPercent;
        private SeasonalOffer seasonalOffer;

        public Builder name(String v) { name = v; return this; }
        public Builder category(String v) { category = v; return this; }
        public Builder region(String v) { region = v; return this; }
        public Builder pricePerKg(double v) { pricePerKg = BigDecimal.valueOf(v); return this; }
        public Builder stockKg(double v) { stockKg = v; return this; }
        public Builder organic(boolean v) { organic = v; return this; }
        public Builder photo(UploadedFile v) { photo = v; return this; }
        public Builder wholesaleDiscount(double percent) { wholesaleDiscountPercent = percent; return this; }
        public Builder seasonalOffer(SeasonalOffer v) { seasonalOffer = v; return this; }

        public ProductRequest build() {
            return new ProductRequest(name, category, region, pricePerKg, stockKg, organic, photo,
                    wholesaleDiscountPercent, seasonalOffer);
        }
    }
}
