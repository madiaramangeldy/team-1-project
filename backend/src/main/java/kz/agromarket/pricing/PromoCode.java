package kz.agromarket.pricing;

import java.time.LocalDate;

public record PromoCode(String code, double percent, LocalDate validUntil) {
}
