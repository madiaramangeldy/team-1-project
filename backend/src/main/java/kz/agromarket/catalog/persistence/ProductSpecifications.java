package kz.agromarket.catalog.persistence;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kz.agromarket.auth.VerificationStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * US-04: каталог сүзгісі.
 *
 * Әр сүзгі тек мәні берілсе ғана WHERE-ге қосылады. Мысалы, тек category берілсе:
 *   WHERE farmer.verification_status = 'APPROVED' AND category.name = 'Көкөніс'
 * Тек верификацияланған фермерлердің өнімдері әрқашан көрінеді.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Каталог сүзгілері. null мән — сүзгі жоқ */
    public record Filter(String category, String region, Boolean organic, BigDecimal minPrice, BigDecimal maxPrice,
                         String q, Long farmerId) {
    }

    public static Specification<ProductEntity> catalog(Filter f) {
        String category = f.category();
        String region = f.region();
        Boolean organic = f.organic();
        BigDecimal minPrice = f.minPrice();
        BigDecimal maxPrice = f.maxPrice();
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(root.get("farmer").get("verificationStatus"), VerificationStatus.APPROVED));
            if (category != null && !category.isBlank()) {
                conditions.add(cb.equal(root.get("category").get("name"), category));
            }
            if (region != null && !region.isBlank()) {
                conditions.add(cb.equal(root.get("region").get("name"), region));
            }
            if (organic != null) {
                conditions.add(cb.equal(root.get("organic"), organic));
            }
            if (minPrice != null) {
                conditions.add(cb.greaterThanOrEqualTo(root.get("pricePerKg"), minPrice));
            }
            if (maxPrice != null) {
                conditions.add(cb.lessThanOrEqualTo(root.get("pricePerKg"), maxPrice));
            }
            if (f.q() != null && !f.q().isBlank()) {
                // Атауы бойынша іздеу, үлкен-кіші әріпке қарамай: lower(name) LIKE '%қыз%'
                conditions.add(cb.like(cb.lower(root.get("name")), "%" + f.q().trim().toLowerCase() + "%"));
            }
            if (f.farmerId() != null) {
                conditions.add(cb.equal(root.get("farmer").get("id"), f.farmerId()));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }
}
