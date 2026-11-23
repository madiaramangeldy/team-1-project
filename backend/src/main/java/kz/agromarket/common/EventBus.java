package kz.agromarket.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OBSERVER паттерні — оқиғалар шинасы.
 *
 * Модуль оқиғаны жариялайды (publish), ал басқа модульдер оны тыңдайды (subscribe).
 * Жариялаушы тыңдаушыларды білмейді, сондықтан модульдер бір-біріне тікелей тәуелді емес.
 *
 * Spring Boot-та бұл ApplicationEventPublisher және @EventListener арқылы ауыстырылады.
 */
public class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add((Consumer<Object>) listener);
    }

    public void publish(Object event) {
        for (Consumer<Object> listener : listeners.getOrDefault(event.getClass(), List.of())) {
            listener.accept(event);
        }
    }
}
