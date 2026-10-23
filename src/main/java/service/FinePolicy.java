package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure fine-policy rules, shared by circulation and automated tests. */
public final class FinePolicy {
    private FinePolicy() { }

    public static BigDecimal assess(LocalDate dueDate, LocalDate returnedOn, BigDecimal dailyRate, BigDecimal cap) {
        if (dueDate == null || returnedOn == null || dailyRate == null || cap == null
                || dailyRate.signum() < 0 || cap.signum() < 0) {
            throw new IllegalArgumentException("Fine policy values must be present and non-negative.");
        }
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnedOn));
        return dailyRate.multiply(BigDecimal.valueOf(overdueDays)).min(cap).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal balanceAfterPayment(BigDecimal assessed, BigDecimal alreadyPaid, BigDecimal payment) {
        if (assessed == null || alreadyPaid == null || payment == null || assessed.signum() < 0
                || alreadyPaid.signum() < 0 || payment.signum() <= 0) {
            throw new IllegalArgumentException("Fine and payment amounts must be valid and non-negative.");
        }
        BigDecimal balance = assessed.subtract(alreadyPaid);
        if (balance.signum() < 0) throw new IllegalStateException("Recorded payments exceed the assessed fine.");
        if (payment.compareTo(balance) > 0) throw new IllegalArgumentException("Payment exceeds the outstanding fine balance.");
        return balance.subtract(payment).setScale(2, RoundingMode.HALF_UP);
    }
}
