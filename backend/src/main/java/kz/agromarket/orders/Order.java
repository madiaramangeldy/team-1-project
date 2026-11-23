package kz.agromarket.orders;

import java.math.BigDecimal;
import java.util.List;
import kz.agromarket.delivery.DeliveryMethod;
import kz.agromarket.payments.PaymentMethod;
import kz.agromarket.pricing.PriceQuote;

public class Order {

    public enum Status { CREATED, AWAITING_CASH, PAID, PAYMENT_FAILED }

    public record Item(String productId, String productName, double quantityKg, PriceQuote price) {
    }

    /** Әр фермерге бөлек ішкі тапсырыс */
    public record SubOrder(String id, String farmerId, List<Item> items, BigDecimal total) {
    }

    private final String id;
    private final String buyerId;
    private final List<SubOrder> subOrders;
    private final DeliveryMethod deliveryMethod;
    private final BigDecimal deliveryCost;
    private final PaymentMethod paymentMethod;
    private final BigDecimal itemsTotal;
    private final BigDecimal total;
    private Status status = Status.CREATED;
    private String transactionId;

    public Order(String id, String buyerId, List<SubOrder> subOrders, DeliveryMethod deliveryMethod,
                 BigDecimal deliveryCost, PaymentMethod paymentMethod, BigDecimal itemsTotal) {
        this.id = id;
        this.buyerId = buyerId;
        this.subOrders = List.copyOf(subOrders);
        this.deliveryMethod = deliveryMethod;
        this.deliveryCost = deliveryCost;
        this.paymentMethod = paymentMethod;
        this.itemsTotal = itemsTotal;
        this.total = itemsTotal.add(deliveryCost);
    }

    public String getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public List<SubOrder> getSubOrders() { return subOrders; }
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public BigDecimal getDeliveryCost() { return deliveryCost; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getItemsTotal() { return itemsTotal; }
    public BigDecimal getTotal() { return total; }
    public Status getStatus() { return status; }
    public String getTransactionId() { return transactionId; }

    void setStatus(Status status) { this.status = status; }
    void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
