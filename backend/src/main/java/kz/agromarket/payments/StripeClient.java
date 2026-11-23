package kz.agromarket.payments;

import java.util.Map;

/**
 * Stripe клиентінің бізге керекті бөлігі.
 * Оның API-і біздің PaymentGateway-ден өзгеше: сома тиынмен (long), валюта кіші әріппен,
 * жауабы — PaymentIntent (id және мәтіндік status). Сондықтан адаптер керек.
 */
public interface StripeClient {

    PaymentIntent createPaymentIntent(long amountInMinorUnits, String currency, Map<String, String> metadata);

    record PaymentIntent(String id, String status) {
    }

    /** Test mode үшін жалған клиент — интернет пен кілтсіз жұмыс істейді */
    class Fake implements StripeClient {
        private int counter;

        @Override
        public PaymentIntent createPaymentIntent(long amountInMinorUnits, String currency, Map<String, String> metadata) {
            counter++;
            String status = amountInMinorUnits > 0 ? "succeeded" : "requires_payment_method";
            return new PaymentIntent("pi_test_" + counter, status);
        }
    }
}
