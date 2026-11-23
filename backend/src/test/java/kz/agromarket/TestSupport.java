package kz.agromarket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import kz.agromarket.auth.RegisterRequest;
import kz.agromarket.auth.Role;
import kz.agromarket.catalog.ProductRequest;

/** Тесттерге ортақ дайындық: верификацияланған 2 фермер, модератор, сатып алушы */
public final class TestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final AgroMarketApp app = new AgroMarketApp();
    public final String farmerA;
    public final String farmerB;
    public final String moderator;
    public final String buyer;

    public TestSupport() {
        farmerA = register("farmerA", Role.FARMER);
        farmerB = register("farmerB", Role.FARMER);
        moderator = register("moderator", Role.MODERATOR);
        buyer = register("buyer", Role.BUYER);
        app.auth.approveFarmer(moderator, farmerA);
        app.auth.approveFarmer(moderator, farmerB);
    }

    public String register(String name, Role role) {
        String email = name + SEQ.incrementAndGet() + "@test.kz";
        return app.auth.register(new RegisterRequest(email, "secret123", name, role)).getId();
    }

    public static ProductRequest.Builder product() {
        return ProductRequest.builder()
                .name("Қызанақ").category("Көкөніс").region("Алматы облысы")
                .pricePerKg(600).stockKg(500).organic(true);
    }

    /** BigDecimal-ды санмен салыстыру (4500 == 4500.00) */
    public static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "күтілгені " + expected + ", алынғаны " + actual);
    }
}
