package kz.agromarket.catalog;

import java.util.List;

public record Page<T>(List<T> items, int total, int page, int pageSize) {
}
