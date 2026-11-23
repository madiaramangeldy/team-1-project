package kz.agromarket.catalog;

import java.math.BigDecimal;

public class Product {

    private final String id;
    private final String farmerId;
    private String name;
    private String category;
    private String region;
    private BigDecimal pricePerKg;
    private double stockKg;
    private boolean organic;
    private final String photoUrl;
    /** 50 кг-нан бастап көтерме жеңілдік, % (0 — жеңілдік жоқ) */
    private double wholesaleDiscountPercent;
    /** Маусымдық жеңілдік (null — жоқ) */
    private SeasonalOffer seasonalOffer;

    public Product(String id, String farmerId, ProductRequest request, String photoUrl) {
        this.id = id;
        this.farmerId = farmerId;
        this.photoUrl = photoUrl;
        apply(request);
    }

    final void apply(ProductRequest request) {
        this.name = request.name();
        this.category = request.category();
        this.region = request.region();
        this.pricePerKg = request.pricePerKg();
        this.stockKg = request.stockKg();
        this.organic = request.organic();
        this.wholesaleDiscountPercent = request.wholesaleDiscountPercent();
        this.seasonalOffer = request.seasonalOffer();
    }

    void decreaseStock(double quantityKg) {
        this.stockKg = Math.max(0, stockKg - quantityKg);
    }

    public String getId() { return id; }
    public String getFarmerId() { return farmerId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getRegion() { return region; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public double getStockKg() { return stockKg; }
    public boolean isOrganic() { return organic; }
    public String getPhotoUrl() { return photoUrl; }
    public double getWholesaleDiscountPercent() { return wholesaleDiscountPercent; }
    public SeasonalOffer getSeasonalOffer() { return seasonalOffer; }
}
