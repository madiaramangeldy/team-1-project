package kz.agromarket.payments;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ADAPTER паттерні (US-10).
 * Stripe API-ін біздің PaymentGateway интерфейсіне келтіреді.
 */
public class StripeAdapter implements PaymentGateway {

    private final StripeClient client;

    public StripeAdapter(StripeClient client) {
        this.client = client;
    }

    @Override
    public String provider() {
        return "stripe";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        // Stripe соманы ең кіші бірлікпен (тиынмен) қабылдайды
        long minorUnits = request.amount().multiply(BigDecimal.valueOf(100)).longValueExact();

        StripeClient.PaymentIntent intent = client.createPaymentIntent(
                minorUnits,
                request.currency().toLowerCase(),
                Map.of("orderId", request.orderId()));

        boolean success = "succeeded".equals(intent.status());
        return new PaymentResult(
                success,
                success ? Status.PAID : Status.FAILED,
                intent.id(),
                provider(),
                success ? null : "Карта арқылы төлем өтпеді");
    }
}
