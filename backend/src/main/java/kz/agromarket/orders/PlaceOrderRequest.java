package kz.agromarket.orders;

import kz.agromarket.delivery.DeliveryMethod;
import kz.agromarket.payments.PaymentMethod;

public record PlaceOrderRequest(
        String buyerRegion,
        DeliveryMethod deliveryMethod,
        double distanceKm,
        PaymentMethod paymentMethod,
        String promoCode) {
}
