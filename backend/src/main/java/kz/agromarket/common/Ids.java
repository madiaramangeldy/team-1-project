package kz.agromarket.common;

import java.util.concurrent.atomic.AtomicLong;

/** Қарапайым ID генераторы. Кейін ID-ді дерекқор (PostgreSQL) жасайды. */
public final class Ids {

    private static final AtomicLong COUNTER = new AtomicLong();

    private Ids() {
    }

    public static String next(String prefix) {
        return prefix + "_" + COUNTER.incrementAndGet();
    }
}
