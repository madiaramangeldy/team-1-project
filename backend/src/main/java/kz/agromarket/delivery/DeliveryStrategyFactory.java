package kz.agromarket.delivery;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import kz.agromarket.common.DomainException;

/**
 * FACTORY METHOD паттерні (US-08, Creational).
 * Таңдалған тәсілге қарай керекті стратегияны жасайды.
 * Жаңа жеткізу тәсілі: жаңа стратегия класы + register() — бар код өзгермейді.
 */
public class DeliveryStrategyFactory {

    private final Map<DeliveryMethod, Supplier<DeliveryCostStrategy>> creators = new EnumMap<>(DeliveryMethod.class);

    public DeliveryStrategyFactory register(DeliveryMethod method, Supplier<DeliveryCostStrategy> creator) {
        creators.put(method, creator);
        return this;
    }

    public DeliveryCostStrategy create(DeliveryMethod method) {
        Supplier<DeliveryCostStrategy> creator = creators.get(method);
        if (creator == null) {
            throw new DomainException("DELIVERY_METHOD_UNKNOWN", "«" + method + "» жеткізу тәсілі қолжетімсіз");
        }
        return creator.get();
    }

    public Set<DeliveryMethod> methods() {
        return creators.keySet();
    }

    public static DeliveryStrategyFactory withDefaults() {
        return new DeliveryStrategyFactory()
                .register(DeliveryMethod.PICKUP, PickupStrategy::new)
                .register(DeliveryMethod.CITY_COURIER, CityCourierStrategy::new)
                .register(DeliveryMethod.INTERCITY, IntercityStrategy::new);
    }
}
