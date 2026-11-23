package kz.agromarket.payments;

import kz.agromarket.common.DomainException;

/**
 * FACTORY METHOD паттерні (US-10, Creational).
 * Төлем тәсілі мен конфигурацияға қарай керекті адаптерді жасайды.
 * OrdersService ешқашан `new StripeAdapter()` жазбайды.
 */
public class PaymentGatewayFactory {

    /** Карта төлемін қай провайдер өңдейді. Ауыстыру тек осы жерде — OrdersService өзгермейді */
    public enum CardProvider { STRIPE }

    private final CardProvider cardProvider;

    public PaymentGatewayFactory(CardProvider cardProvider) {
        this.cardProvider = cardProvider;
    }

    public PaymentGateway create(PaymentMethod method) {
        return switch (method) {
            case CASH -> new CashOnDeliveryAdapter();
            case CARD -> createCardGateway();
        };
    }

    private PaymentGateway createCardGateway() {
        if (cardProvider == CardProvider.STRIPE) {
            return new StripeAdapter(new StripeClient.Fake());
        }
        throw new DomainException("PAYMENT_PROVIDER_UNKNOWN", "Төлем провайдері бапталмаған");
    }
}
