package kz.agromarket;

import java.math.BigDecimal;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.UserEntity;
import kz.agromarket.catalog.persistence.CategoryEntity;
import kz.agromarket.catalog.persistence.ProductEntity;
import kz.agromarket.catalog.persistence.RegionEntity;

/**
 * JPA-сервистер тесттеріне арналған дайын объектілер.
 * Дерекқор жоқ, сондықтан id-ді (әдетте оны PostgreSQL береді) рефлексиямен қолмен қоямыз.
 */
final class JpaFixtures {

    private JpaFixtures() {
    }

    static <T> T withId(T entity, long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static UserEntity user(long id, String email, Role role) {
        return withId(new UserEntity(email, "hash", email, null, role), id);
    }

    static FarmerProfileEntity approvedFarmer(long id, String farmName, String region) {
        FarmerProfileEntity farmer = new FarmerProfileEntity(
                user(100 + id, farmName + "@farm.kz", Role.FARMER), new RegionEntity(region), farmName);
        farmer.approve();
        return withId(farmer, id);
    }

    static ProductEntity product(long id, FarmerProfileEntity farmer, String name, int pricePerKg, int stockKg) {
        return withId(new ProductEntity(farmer, new CategoryEntity("Көкөніс"), farmer.getRegion(), name,
                BigDecimal.valueOf(pricePerKg), BigDecimal.valueOf(stockKg), false), id);
    }

    static BigDecimal kg(double value) {
        return BigDecimal.valueOf(value);
    }
}
