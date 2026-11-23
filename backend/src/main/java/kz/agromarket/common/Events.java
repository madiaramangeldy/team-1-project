package kz.agromarket.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Модульдер арасындағы оқиғалар (Observer). */
public final class Events {

    private Events() {
    }

    /** US-11, US-12: тапсырыс төленді */
    public record OrderPaid(String orderId, String buyerId, LocalDate paidAt, List<PaidSubOrder> subOrders) {
    }

    public record PaidSubOrder(String farmerId, BigDecimal total, List<PaidItem> items) {
    }

    public record PaidItem(String productId, String productName, double quantityKg, BigDecimal total) {
    }

    /** US-02: модератор фермер туралы шешім қабылдады */
    public record FarmerVerified(String farmerId, boolean approved, String reason) {
    }
}
