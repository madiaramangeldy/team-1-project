package kz.agromarket.notifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import kz.agromarket.common.EventBus;
import kz.agromarket.common.Events;

/**
 * Хабарламалар. Қазір жадта сақталады, кейін email/push арқылы жіберіледі.
 *
 * OBSERVER — тыңдаушы (US-02, US-11).
 * Orders және Auth модульдері бұл класты білмейді, тек оқиға жариялайды.
 */
public class NotificationService {

    public record Notification(String userId, String text) {
    }

    private final List<Notification> sent = new ArrayList<>();

    public void send(String userId, String text) {
        sent.add(new Notification(userId, text));
    }

    public List<Notification> forUser(String userId) {
        return sent.stream().filter(n -> n.userId().equals(userId)).toList();
    }

    public void subscribeToEvents(EventBus eventBus) {
        eventBus.subscribe(Events.OrderPaid.class, this::onOrderPaid);
        eventBus.subscribe(Events.FarmerVerified.class, this::onFarmerVerified);
    }

    private void onOrderPaid(Events.OrderPaid event) {
        for (Events.PaidSubOrder sub : event.subOrders()) {
            double weight = sub.items().stream().mapToDouble(Events.PaidItem::quantityKg).sum();
            send(sub.farmerId(), "Жаңа төленген тапсырыс " + event.orderId() + ": " + (long) weight + " кг, "
                    + plain(sub.total()) + " ₸. Өнімді дайындаңыз.");
        }
    }

    private void onFarmerVerified(Events.FarmerVerified event) {
        send(event.farmerId(), event.approved()
                ? "Құжаттарыңыз расталды. Енді өнім қоса аласыз."
                : "Верификациядан өтпедіңіз. Себебі: " + event.reason());
    }

    private static String plain(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
