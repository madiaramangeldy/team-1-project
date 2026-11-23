package kz.agromarket.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kz.agromarket.auth.AuthService;
import kz.agromarket.common.DomainException;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.Events;
import kz.agromarket.common.Ids;

/**
 * US-03 Өнім қосу, US-04 Каталогты сүзу.
 */
public class CatalogService {

    private static final List<String> PHOTO_EXTENSIONS = List.of("jpg", "jpeg", "png");

    private final Map<String, Product> products = new LinkedHashMap<>();
    private final AuthService auth;
    private final PhotoStorage storage;

    public CatalogService(AuthService auth, PhotoStorage storage) {
        this.auth = auth;
        this.storage = storage;
    }

    // ---------- US-03 ----------

    public Product addProduct(String farmerId, ProductRequest request) {
        if (!auth.isVerifiedFarmer(farmerId)) {
            throw new DomainException("FARMER_NOT_VERIFIED", "Өнім қосу үшін верификациядан өту керек", 403);
        }
        validate(request);
        request.photo().validate(PHOTO_EXTENSIONS, "Фото JPG немесе PNG форматында болуы керек");

        String photoUrl = storage.upload(farmerId, request.photo().fileName());
        Product product = new Product(Ids.next("product"), farmerId, request, photoUrl);
        products.put(product.getId(), product);
        return product;
    }

    public Product updateProduct(String farmerId, String productId, ProductRequest request) {
        Product product = getOwnProduct(farmerId, productId);
        validate(request);
        product.apply(request);
        return product;
    }

    public void deleteProduct(String farmerId, String productId) {
        getOwnProduct(farmerId, productId);
        products.remove(productId);
    }

    // ---------- US-04 ----------

    public Page<Product> search(CatalogFilter filter) {
        List<Product> matched = products.values().stream()
                .filter(p -> auth.isVerifiedFarmer(p.getFarmerId()))
                .filter(filter::matches)
                .toList();

        int from = Math.min((filter.getPage() - 1) * filter.getPageSize(), matched.size());
        int to = Math.min(from + filter.getPageSize(), matched.size());
        return new Page<>(new ArrayList<>(matched.subList(from, to)), matched.size(), filter.getPage(), filter.getPageSize());
    }

    // ---------- Көмекші әдістер ----------

    public Product getProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new DomainException("NOT_FOUND", "Өнім табылмады", 404);
        }
        return product;
    }

    /**
     * US-11. OBSERVER — тыңдаушы: төлемнен кейін өнім қалдығын азайтады.
     * Orders модулі бұл әдісті тікелей шақырмайды, тек оқиға жариялайды.
     */
    public void subscribeToEvents(EventBus eventBus) {
        eventBus.subscribe(Events.OrderPaid.class, event -> event.subOrders().forEach(sub ->
                sub.items().forEach(item -> getProduct(item.productId()).decreaseStock(item.quantityKg()))));
    }

    private Product getOwnProduct(String farmerId, String productId) {
        Product product = getProduct(productId);
        if (!product.getFarmerId().equals(farmerId)) {
            throw new DomainException("FORBIDDEN", "Тек өз өніміңізді өзгерте аласыз", 403);
        }
        return product;
    }

    private static void validate(ProductRequest r) {
        List<String> missing = new ArrayList<>();
        if (isBlank(r.name())) missing.add("name");
        if (isBlank(r.category())) missing.add("category");
        if (isBlank(r.region())) missing.add("region");
        if (!missing.isEmpty()) {
            throw new DomainException("VALIDATION", "Міндетті өрістер толтырылмаған: " + String.join(", ", missing));
        }
        if (r.pricePerKg() == null || r.pricePerKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("VALIDATION", "1 кг бағасы 0-ден үлкен болуы керек");
        }
        if (r.stockKg() < 0) {
            throw new DomainException("VALIDATION", "Қалдық теріс бола алмайды");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
