package kz.agromarket.catalog;

import java.time.LocalDate;

/** Маусымдық жеңілдік: пайызы және мерзімі */
public record SeasonalOffer(double percent, LocalDate from, LocalDate to) {

    public boolean isActive(LocalDate today) {
        return !today.isBefore(from) && !today.isAfter(to);
    }
}
