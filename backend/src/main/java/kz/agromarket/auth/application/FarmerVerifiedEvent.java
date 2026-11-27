package kz.agromarket.auth.application;

import kz.agromarket.auth.persistence.FarmerProfileEntity;

/** Observer оқиғасы: модератор фермерді растады немесе бас тартты (US-02) */
public record FarmerVerifiedEvent(FarmerProfileEntity farmer) {
}
