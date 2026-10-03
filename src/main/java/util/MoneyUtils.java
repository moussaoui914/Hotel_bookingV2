package util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static BigDecimal calculateTotal(BigDecimal pricePerNight, long nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal format(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal applyPercentage(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).divide(new BigDecimal("100"), SCALE, ROUNDING);
    }

    public static BigDecimal addPercentage(BigDecimal amount, BigDecimal percentage) {
        BigDecimal adjustment = applyPercentage(amount, percentage);
        return amount.add(adjustment).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal subtractPercentage(BigDecimal amount, BigDecimal percentage) {
        BigDecimal adjustment = applyPercentage(amount, percentage);
        return amount.subtract(adjustment).setScale(SCALE, ROUNDING);
    }

    public static String formatWithCurrency(BigDecimal amount, String currency) {
        return format(amount) + " " + currency;
    }
}