package kz.agromarket.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Пагинация жауабы. Spring-тің Page объектісін тікелей қайтармаймыз:
 * оның JSON-ы ауыр әрі тұрақсыз. Мұнда тек керегі ғана.
 * page — 1-ден басталады (пайдаланушыға түсінікті).
 */
public record PageResponse<T>(List<T> items, long total, int page, int size, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(),
                page.getNumber() + 1, page.getSize(), page.getTotalPages());
    }
}
