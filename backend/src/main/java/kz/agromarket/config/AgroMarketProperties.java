package kz.agromarket.config;

import java.util.List;
import kz.agromarket.payments.PaymentGatewayFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml-дағы `agromarket:` бөлімі.
 * Баптауларды кодтан бөлек ұстаймыз: өңірлер тізімін немесе төлем провайдерін
 * ауыстыру үшін Java кодын өзгерту керек емес.
 */
@ConfigurationProperties(prefix = "agromarket")
public record AgroMarketProperties(List<String> supportedRegions, Payment payment) {

    public record Payment(PaymentGatewayFactory.CardProvider cardProvider) {
    }
}
