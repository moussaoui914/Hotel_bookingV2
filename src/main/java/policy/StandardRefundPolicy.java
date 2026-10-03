package policy;

import model.Reservation;
import util.DateUtils;
import util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRefundPolicy implements RefundPolicy {

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        long daysBeforeCheckIn = DateUtils.daysBetween(LocalDate.now(), reservation.getCheckIn());
        BigDecimal total = reservation.getTotalPrice();

        if (daysBeforeCheckIn > 14) {
            return MoneyUtils.format(total);
        } else if (daysBeforeCheckIn >= 7) {
            return MoneyUtils.subtractPercentage(total, new BigDecimal("30"));
        } else if (daysBeforeCheckIn >= 2) {
            return MoneyUtils.subtractPercentage(total, new BigDecimal("50"));
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String getPolicyDescription() {
        return """
            Politique de remboursement:
            - Plus de 14 jours avant check-in : 100% remboursé
            - Entre 7 et 14 jours : 70% remboursé
            - Entre 48h et 7 jours : 50% remboursé
            - Moins de 48h : aucun remboursement
            """;
    }
}