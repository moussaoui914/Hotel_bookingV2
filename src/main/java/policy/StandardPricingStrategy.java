package policy;

import model.Room;
import util.DateUtils;
import util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardPricingStrategy implements PricingStrategy {

    private static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("30");
    private static final BigDecimal LOW_SEASON_DISCOUNT = new BigDecimal("15");
    private static final BigDecimal WEEKEND_SURCHARGE = new BigDecimal("15");
    private static final BigDecimal LONG_STAY_7_DISCOUNT = new BigDecimal("10");
    private static final BigDecimal LONG_STAY_14_DISCOUNT = new BigDecimal("15");
    private static final BigDecimal EARLY_BOOKING_DISCOUNT = new BigDecimal("5");
    private static final BigDecimal LAST_MINUTE_SURCHARGE = new BigDecimal("10");

    @Override
    public BigDecimal calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, LocalDate reservationDate) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal basePrice = room.getPricePerNight();
        BigDecimal total = BigDecimal.ZERO;

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightlyPrice = basePrice;

            if (DateUtils.isHighSeason(current)) {
                nightlyPrice = MoneyUtils.addPercentage(nightlyPrice, HIGH_SEASON_SURCHARGE);
            } else if (DateUtils.isLowSeason(current)) {
                nightlyPrice = MoneyUtils.subtractPercentage(nightlyPrice, LOW_SEASON_DISCOUNT);
            }

            if (DateUtils.isWeekend(current)) {
                nightlyPrice = MoneyUtils.addPercentage(nightlyPrice, WEEKEND_SURCHARGE);
            }

            total = total.add(nightlyPrice);
            current = current.plusDays(1);
        }

        if (nights >= 14) {
            total = MoneyUtils.subtractPercentage(total, LONG_STAY_14_DISCOUNT);
        } else if (nights >= 7) {
            total = MoneyUtils.subtractPercentage(total, LONG_STAY_7_DISCOUNT);
        }

        long daysBeforeCheckIn = ChronoUnit.DAYS.between(reservationDate, checkIn);
        if (daysBeforeCheckIn >= 30) {
            total = MoneyUtils.subtractPercentage(total, EARLY_BOOKING_DISCOUNT);
        } else if (daysBeforeCheckIn <= 3 && daysBeforeCheckIn >= 0) {
            total = MoneyUtils.addPercentage(total, LAST_MINUTE_SURCHARGE);
        }

        return MoneyUtils.format(total);
    }
}