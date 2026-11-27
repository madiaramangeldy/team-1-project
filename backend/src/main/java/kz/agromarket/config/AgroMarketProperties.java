package kz.agromarket.config;

import java.util.List;
import kz.agromarket.payments.PaymentGatewayFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml-дағы `agromarket:` бөлімі.
 * Баптауларды кодтан бөлек ұстаймыз: өңірлер тізімін, төлем провайдерін немесе JWT мерзімін
 * ауыстыру үшін Java кодын өзгерту керек емес.
 */
@ConfigurationProperties(prefix = "agromarket")
public record AgroMarketProperties(
        List<String> supportedRegions,
        Payment payment,
        Jwt jwt,
        Storage storage,
        boolean seedDemoData) {

    public record Payment(PaymentGatewayFactory.CardProvider cardProvider) {
    }

    /** JWT баптаулары: secret кемінде 32 таңба, ttlMinutes — токеннің жарамдылық мерзімі */
    public record Jwt(String secret, long ttlMinutes) {
    }

    /** MinIO баптаулары. endpoint бос болса — файлдар жадта сақталады (тест режимі) */
    public record Storage(String endpoint, String accessKey, String secretKey, String bucket) {
    }
}
