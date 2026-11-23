package kz.agromarket.catalog;

import java.math.BigDecimal;

/** Каталог сүзгісі. null өріс — бұл сүзгі қолданылмайды. */
public class CatalogFilter {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private String category;
    private String region;
    private Boolean organic;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    public CatalogFilter category(String v) { category = v; return this; }
    public CatalogFilter region(String v) { region = v; return this; }
    public CatalogFilter organic(boolean v) { organic = v; return this; }
    public CatalogFilter minPrice(double v) { minPrice = BigDecimal.valueOf(v); return this; }
    public CatalogFilter maxPrice(double v) { maxPrice = BigDecimal.valueOf(v); return this; }
    public CatalogFilter page(int v) { page = Math.max(1, v); return this; }

    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }

    boolean matches(Product p) {
        return (category == null || category.equals(p.getCategory()))
                && (region == null || region.equals(p.getRegion()))
                && (organic == null || organic == p.isOrganic())
                && (minPrice == null || p.getPricePerKg().compareTo(minPrice) >= 0)
                && (maxPrice == null || p.getPricePerKg().compareTo(maxPrice) <= 0);
    }
}
