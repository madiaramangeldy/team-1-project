package kz.agromarket.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.catalog.SeasonalOffer;
import kz.agromarket.pricing.PricedProduct;

/**
 * `products` кестесі. Байланыстар (барлығы N : 1):
 *  - фермерге: бір фермерде көп өнім
 *  - санатқа және өңірге: каталогты сүзу үшін (US-04)
 */
@Entity
@Table(name = "products")
public class ProductEntity implements PricedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private FarmerProfileEntity farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private RegionEntity region;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "price_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "stock_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockKg = BigDecimal.ZERO;

    @Column(name = "organic", nullable = false)
    private boolean organic;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /** US-06: 50 кг-нан бастап көтерме жеңілдік, % */
    @Column(name = "wholesale_discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal wholesaleDiscountPercent = BigDecimal.ZERO;

    /** US-07: маусымдық жеңілдік */
    @Column(name = "seasonal_discount_percent", precision = 5, scale = 2)
    private BigDecimal seasonalDiscountPercent;

    @Column(name = "seasonal_from")
    private LocalDate seasonalFrom;

    @Column(name = "seasonal_to")
    private LocalDate seasonalTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProductEntity() {
        // JPA үшін бос конструктор міндетті
    }

    public ProductEntity(FarmerProfileEntity farmer, CategoryEntity category, RegionEntity region,
                         String name, BigDecimal pricePerKg, BigDecimal stockKg, boolean organic) {
        this.farmer = farmer;
        this.category = category;
        this.region = region;
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.stockKg = stockKg;
        this.organic = organic;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---------- Өзгерту әдістері ----------

    public void update(CategoryEntity category, RegionEntity region, String name,
                       BigDecimal pricePerKg, BigDecimal stockKg, boolean organic) {
        this.category = category;
        this.region = region;
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.stockKg = stockKg;
        this.organic = organic;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setDiscounts(BigDecimal wholesalePercent, BigDecimal seasonalPercent, LocalDate from, LocalDate to) {
        this.wholesaleDiscountPercent = wholesalePercent == null ? BigDecimal.ZERO : wholesalePercent;
        this.seasonalDiscountPercent = seasonalPercent;
        this.seasonalFrom = from;
        this.seasonalTo = to;
    }

    /** US-11: төлемнен кейін қалдықты азайту */
    public void decreaseStock(BigDecimal quantityKg) {
        this.stockKg = stockKg.subtract(quantityKg).max(BigDecimal.ZERO);
    }

    public Long getId() { return id; }
    public FarmerProfileEntity getFarmer() { return farmer; }
    public CategoryEntity getCategory() { return category; }
    public RegionEntity getRegion() { return region; }
    public String getName() { return name; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public BigDecimal getStockKg() { return stockKg; }
    public boolean isOrganic() { return organic; }
    public String getPhotoUrl() { return photoUrl; }
    public BigDecimal getWholesaleDiscountPercent() { return wholesaleDiscountPercent; }
    public BigDecimal getSeasonalDiscountPercent() { return seasonalDiscountPercent; }
    public LocalDate getSeasonalFrom() { return seasonalFrom; }
    public LocalDate getSeasonalTo() { return seasonalTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ---------- PricedProduct: паттерндер осы әдістер арқылы жұмыс істейді ----------

    @Override
    public double availableKg() {
        return stockKg.doubleValue();
    }

    @Override
    public double wholesalePercent() {
        return wholesaleDiscountPercent.doubleValue();
    }

    @Override
    public SeasonalOffer seasonalOffer() {
        if (seasonalDiscountPercent == null || seasonalFrom == null || seasonalTo == null) {
            return null;
        }
        return new SeasonalOffer(seasonalDiscountPercent.doubleValue(), seasonalFrom, seasonalTo);
    }
}
