package kz.agromarket.payments;

import java.math.BigDecimal;

/**
 * Төлемнің бірыңғай интерфейсі. OrdersService тек осы интерфейсті біледі.
 * Кез келген адаптер бірдей PaymentResult қайтарады (SOLID — L).
 */
public interface PaymentGateway {

    String provider();

    PaymentResult pay(PaymentRequest request);

    record PaymentRequest(String orderId, BigDecimal amount, String currency) {
    }

    /** PAID — төленді; PENDING — курьер қолма-қол алғаннан кейін расталады */
    enum Status { PAID, PENDING, FAILED }

    record PaymentResult(boolean success, Status status, String transactionId, String provider, String message) {
    }
}
