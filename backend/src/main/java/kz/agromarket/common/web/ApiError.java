package kz.agromarket.common.web;

import java.time.Instant;
import java.util.Map;

/**
 * API-дің бірыңғай қате форматы. Кез келген қате осы JSON түрінде қайтарылады:
 *
 * <pre>
 * {
 *   "code": "MIN_WEIGHT",
 *   "message": "Минималды салмақ — 5 кг. Сіздің тапсырысыңыз: 3 кг",
 *   "status": 400,
 *   "path": "/api/orders",
 *   "timestamp": "2026-11-25T10:00:00Z",
 *   "fieldErrors": {}
 * }
 * </pre>
 */
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    public static ApiError of(String code, String message, int status, String path) {
        return new ApiError(code, message, status, path, Instant.now(), Map.of());
    }
}
