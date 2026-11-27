package kz.agromarket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import kz.agromarket.auth.Role;

/**
 * JWT токен жасау және тексеру.
 *
 * Токен үш бөліктен тұрады: header.payload.signature.
 * Payload-та пайдаланушы ID-і, email және рөлі бар. Signature құпия кілтпен (HMAC-SHA256) қол қойылады:
 * токенді біреу өзгертсе, қолтаңба сәйкес келмейді және токен қабылданбайды.
 *
 * Сервер сессия сақтамайды (stateless): әр сұрауда токеннің өзі тексеріледі.
 */
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 үшін кемінде 256 бит

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(String secret, long ttlMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret кемінде 32 байт болуы керек");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Кіргеннен кейін токен береді */
    public String issue(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Токен дұрыс әрі мерзімі өтпеген болса — пайдаланушы, әйтпесе бос */
    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
