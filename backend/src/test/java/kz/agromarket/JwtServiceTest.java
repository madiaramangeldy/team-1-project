package kz.agromarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kz.agromarket.auth.Role;
import kz.agromarket.security.AuthUser;
import kz.agromarket.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("US-01. JWT токен")
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";
    private final JwtService jwt = new JwtService(SECRET, 60);

    @Test
    @DisplayName("US-01: токеннен пайдаланушы мен рөл дұрыс оқылады")
    void roundTrip() {
        String token = jwt.issue(new AuthUser(7L, "aidar@farm.kz", Role.FARMER));

        AuthUser user = jwt.parse(token).orElseThrow();
        assertEquals(7L, user.id());
        assertEquals("aidar@farm.kz", user.email());
        assertEquals(Role.FARMER, user.role());
    }

    @Test
    @DisplayName("US-01: өзгертілген токен қабылданбайды")
    void tamperedTokenRejected() {
        String token = jwt.issue(new AuthUser(7L, "aidar@farm.kz", Role.FARMER));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertTrue(jwt.parse(tampered).isEmpty());
        assertTrue(jwt.parse("not-a-token").isEmpty());
    }

    @Test
    @DisplayName("US-01: басқа кілтпен қол қойылған токен қабылданбайды")
    void foreignKeyRejected() {
        JwtService other = new JwtService("another-secret-key-also-32-bytes-long!!", 60);
        String foreign = other.issue(new AuthUser(1L, "admin@agromarket.kz", Role.MODERATOR));

        assertTrue(jwt.parse(foreign).isEmpty());
    }

    @Test
    @DisplayName("US-01: мерзімі өткен токен қабылданбайды")
    void expiredTokenRejected() {
        JwtService shortLived = new JwtService(SECRET, -1); // мерзімі бірден өткен
        String token = shortLived.issue(new AuthUser(7L, "aidar@farm.kz", Role.FARMER));

        assertTrue(jwt.parse(token).isEmpty());
    }

    @Test
    @DisplayName("US-01: тым қысқа кілт рұқсат етілмейді")
    void shortSecretRejected() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60));
    }
}
