package kz.agromarket.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import kz.agromarket.auth.Role;
import kz.agromarket.auth.persistence.FarmerProfileEntity;
import kz.agromarket.auth.persistence.FarmerProfileRepository;
import kz.agromarket.auth.persistence.UserEntity;
import kz.agromarket.auth.persistence.UserRepository;
import kz.agromarket.catalog.persistence.CategoryEntity;
import kz.agromarket.catalog.persistence.CategoryRepository;
import kz.agromarket.catalog.persistence.ProductEntity;
import kz.agromarket.catalog.persistence.ProductRepository;
import kz.agromarket.catalog.persistence.RegionEntity;
import kz.agromarket.catalog.persistence.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Демо деректер (ER.md-дағы «Seed жоспары»).
 *
 * Пайдаланушылар SQL миграциясында емес, осында жасалады: құпиясөздер BCrypt-пен хэштелуі керек.
 * Қосымша іске қосылғанда бір рет орындалады: users кестесі бос болмаса, ештеңе жасамайды.
 * Өшіру: SEED_DEMO_DATA=false.
 *
 * Демо аккаунттар (құпиясөз: secret123):
 *   admin@agromarket.kz  — MODERATOR
 *   aidar@farm.kz        — FARMER, верификацияланған (Алматы облысы)
 *   gulnar@farm.kz       — FARMER, верификацияланған (Астана)
 *   asqar@farm.kz        — FARMER, тексеруді күтіп тұр (модерация демосы үшін)
 *   cafe@mail.kz         — BUYER
 *   courier@agromarket.kz — COURIER (қолма-қол төлемді растайды)
 */
@Component
@ConditionalOnProperty(name = "agromarket.seed-demo-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "secret123";

    private final UserRepository users;
    private final FarmerProfileRepository farmers;
    private final RegionRepository regions;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository users, FarmerProfileRepository farmers, RegionRepository regions,
                      CategoryRepository categories, ProductRepository products, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.farmers = farmers;
        this.regions = regions;
        this.categories = categories;
        this.products = products;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return; // бұрын толтырылған — қайталамаймыз
        }

        user("admin@agromarket.kz", "Модератор", Role.MODERATOR);
        user("cafe@mail.kz", "«Дастархан» кафесі", Role.BUYER);
        user("courier@agromarket.kz", "Курьер Ерлан", Role.COURIER);

        FarmerProfileEntity aidar = farmer("aidar@farm.kz", "Айдар", "Айдар фермасы", "Алматы облысы", true);
        FarmerProfileEntity gulnar = farmer("gulnar@farm.kz", "Гүлнар", "Гүлнар бау-бақшасы", "Астана", true);
        farmer("asqar@farm.kz", "Асқар", "Асқар қожалығы", "Шымкент", false);

        LocalDate today = LocalDate.now();
        ProductEntity tomato = product(aidar, "Көкөніс", "Қызанақ", 600, 500, true);
        tomato.setDiscounts(BigDecimal.TEN, BigDecimal.valueOf(5), today.withDayOfMonth(1), today.plusMonths(2));
        product(aidar, "Жеміс", "Алма", 450, 300, false);
        ProductEntity potato = product(gulnar, "Көкөніс", "Картоп", 250, 1000, false);
        potato.setDiscounts(BigDecimal.valueOf(8), null, null, null);
        product(gulnar, "Сүт өнімдері", "Сиыр сүті", 400, 200, true);
        product(gulnar, "Бал", "Таулы бал", 4500, 50, true);

        log.info("Demo data created: 6 users, 5 products (password: {})", DEMO_PASSWORD);
    }

    private UserEntity user(String email, String name, Role role) {
        return users.save(new UserEntity(email, passwordEncoder.encode(DEMO_PASSWORD), name, null, role));
    }

    private FarmerProfileEntity farmer(String email, String name, String farmName, String region, boolean approved) {
        UserEntity user = user(email, name, Role.FARMER);
        FarmerProfileEntity profile = new FarmerProfileEntity(user, region(region), farmName);
        profile.uploadDocument("minio://documents/" + email + "/kualik.pdf");
        if (approved) {
            profile.approve();
        }
        return farmers.save(profile);
    }

    private ProductEntity product(FarmerProfileEntity farmer, String category, String name,
                                  double pricePerKg, double stockKg, boolean organic) {
        return products.save(new ProductEntity(farmer, category(category), farmer.getRegion(), name,
                BigDecimal.valueOf(pricePerKg), BigDecimal.valueOf(stockKg), organic));
    }

    private RegionEntity region(String name) {
        return regions.findByName(name).orElseThrow(() -> new IllegalStateException("Region missing: " + name));
    }

    private CategoryEntity category(String name) {
        return categories.findByName(name).orElseThrow(() -> new IllegalStateException("Category missing: " + name));
    }
}
