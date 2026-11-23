package kz.agromarket.payments;

/**
 * ADAPTER паттерні (US-10).
 * Қолма-қол төлемде сыртқы API жоқ, бірақ ол да сол PaymentGateway интерфейсін іске асырады.
 * Курьер ақшаны алғанша мәртебе — PENDING.
 */
public class CashOnDeliveryAdapter implements PaymentGateway {

    @Override
    public String provider() {
        return "cash";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        return new PaymentResult(true, Status.PENDING, "cod_" + request.orderId(), provider(),
                "Курьерге қолма-қол төленеді");
    }
}
