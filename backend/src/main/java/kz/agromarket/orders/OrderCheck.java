package kz.agromarket.orders;

/**
 * CHAIN OF RESPONSIBILITY паттерні (US-09).
 *
 * Әр тексеру — тізбектің бір буыны. Буын өз тексерісін жасайды:
 *  - қате болса — тізбекті тоқтатып, түсінікті қате лақтырады (DomainException);
 *  - бәрі дұрыс болса — келесі буынға береді.
 *
 * Жаңа тексеру = жаңа класс + setNext(). OrdersService өзгермейді (SOLID — O).
 */
public abstract class OrderCheck {

    private OrderCheck next;

    /** Келесі буынды тіркейді және оны қайтарады: a.setNext(b).setNext(c) */
    public OrderCheck setNext(OrderCheck next) {
        this.next = next;
        return next;
    }

    public final void handle(OrderDraft draft) {
        check(draft);
        if (next != null) {
            next.handle(draft);
        }
    }

    protected abstract void check(OrderDraft draft);
}
