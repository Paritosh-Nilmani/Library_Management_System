package service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinePolicyTest {
    @Test
    void noFineIsAssessedOnOrBeforeDueDate() {
        assertEquals(new BigDecimal("0.00"), FinePolicy.assess(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"),
                new BigDecimal("5.00"), new BigDecimal("100.00")));
    }

    @Test
    void calculatesOverdueFineUsingDecimalArithmetic() {
        assertEquals(new BigDecimal("15.00"), FinePolicy.assess(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-04"),
                new BigDecimal("5.00"), new BigDecimal("100.00")));
    }

    @Test
    void capsFineAtConfiguredMaximum() {
        assertEquals(new BigDecimal("20.00"), FinePolicy.assess(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01"),
                new BigDecimal("5.00"), new BigDecimal("20.00")));
    }

    @Test
    void rejectsNegativeRates() {
        assertThrows(IllegalArgumentException.class, () -> FinePolicy.assess(
                LocalDate.now(), LocalDate.now(), new BigDecimal("-1.00"), new BigDecimal("20.00")));
    }

    @Test
    void acceptsPartialAndCompletePayments() {
        assertEquals(new BigDecimal("7.50"), FinePolicy.balanceAfterPayment(
                new BigDecimal("10.00"), new BigDecimal("0.00"), new BigDecimal("2.50")));
        assertEquals(new BigDecimal("0.00"), FinePolicy.balanceAfterPayment(
                new BigDecimal("10.00"), new BigDecimal("7.50"), new BigDecimal("2.50")));
    }

    @Test
    void rejectsOverpayment() {
        assertThrows(IllegalArgumentException.class, () -> FinePolicy.balanceAfterPayment(
                new BigDecimal("10.00"), new BigDecimal("3.00"), new BigDecimal("7.01")));
    }
}
