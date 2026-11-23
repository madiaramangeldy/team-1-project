package kz.agromarket.orders;

import java.util.List;
import kz.agromarket.common.DomainException;
import kz.agromarket.delivery.DeliveryMethod;

/**
 * Тізбектің 4 буыны (US-09): Stock → MinWeight → Region → Fraud
 */
public final class OrderChecks {

    private OrderChecks() {
    }

    /** Тізбекті құрастыру. Ретін ауыстыру немесе буын қосу — тек осы жерде. */
    public static OrderCheck build(List<String> supportedRegions) {
        OrderCheck first = new StockCheck();
        first.setNext(new MinWeightCheck(5))
                .setNext(new RegionCheck(supportedRegions))
                .setNext(new FraudCheck(2000));
        return first;
    }

    /** 1. Қоймада жеткілікті ме */
    public static class StockCheck extends OrderCheck {
        @Override
        protected void check(OrderDraft draft) {
            for (OrderDraft.Line line : draft.lines()) {
                if (line.quantityKg() > line.product().getStockKg()) {
                    throw new DomainException("OUT_OF_STOCK", "«" + line.product().getName()
                            + "» қоймада жеткіліксіз: бар болғаны " + kg(line.product().getStockKg())
                            + " кг, сіз " + kg(line.quantityKg()) + " кг сұрадыңыз");
                }
            }
        }
    }

    /** 2. Минималды салмақ */
    public static class MinWeightCheck extends OrderCheck {
        private final double minKg;

        public MinWeightCheck(double minKg) {
            this.minKg = minKg;
        }

        @Override
        protected void check(OrderDraft draft) {
            if (draft.totalWeightKg() < minKg) {
                throw new DomainException("MIN_WEIGHT", "Минималды салмақ — " + kg(minKg)
                        + " кг. Сіздің тапсырысыңыз: " + kg(draft.totalWeightKg()) + " кг");
            }
        }
    }

    /** 3. Өңірге жеткізу бар ма */
    public static class RegionCheck extends OrderCheck {
        private final List<String> supportedRegions;

        public RegionCheck(List<String> supportedRegions) {
            this.supportedRegions = supportedRegions;
        }

        @Override
        protected void check(OrderDraft draft) {
            if (draft.deliveryMethod() == DeliveryMethod.PICKUP) {
                return; // өзі алып кетсе, өңір маңызды емес
            }
            if (!supportedRegions.contains(draft.buyerRegion())) {
                throw new DomainException("REGION_NOT_SUPPORTED",
                        "«" + draft.buyerRegion() + "» өңіріне жеткізу әзірге жоқ");
            }
        }
    }

    /** 4. Күдікті тапсырыс (тым үлкен көлем) */
    public static class FraudCheck extends OrderCheck {
        private final double maxWeightKg;

        public FraudCheck(double maxWeightKg) {
            this.maxWeightKg = maxWeightKg;
        }

        @Override
        protected void check(OrderDraft draft) {
            if (draft.totalWeightKg() > maxWeightKg) {
                throw new DomainException("FRAUD_SUSPECTED",
                        kg(maxWeightKg) + " кг-нан асатын тапсырысты модератор тексеруі керек");
            }
        }
    }

    /** 5.0 → "5", 2.5 → "2.5" */
    static String kg(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
