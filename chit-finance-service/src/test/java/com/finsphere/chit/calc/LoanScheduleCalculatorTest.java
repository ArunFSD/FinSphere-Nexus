package com.finsphere.chit.calc;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.finsphere.chit.calc.LoanScheduleCalculator.LoanTerms;
import com.finsphere.common.dto.finance.LeaveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanScheduleCalculatorTest {

    /** Monday 5 January 2026. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

    @Test
    void termsDeductInterestUpFrontAndSpreadThePrincipalOverTheTenure() {
        LoanTerms terms = LoanScheduleCalculator.terms(new BigDecimal("10000"), new BigDecimal("1000"), 100, MONDAY,
                LeaveType.NONE);

        assertEquals(new BigDecimal("9000.00"), terms.disbursedAmount());
        assertEquals(new BigDecimal("100.00"), terms.dailyInstallment());
        assertEquals(MONDAY.plusDays(99), terms.endDate());
    }

    @Test
    void dailyInstallmentIsRoundedToPaise() {
        LoanTerms terms = LoanScheduleCalculator.terms(new BigDecimal("10000"), BigDecimal.ZERO, 3, MONDAY,
                LeaveType.NONE);
        assertEquals(new BigDecimal("3333.33"), terms.dailyInstallment());
    }

    @Test
    void sundayLeaveSkipsSundays() {
        // Mon..Sat = 6 collection days, the 7th falls on the next Monday
        assertEquals(MONDAY.plusDays(7), LoanScheduleCalculator.collectionEndDate(MONDAY, 7, LeaveType.SUNDAY));
    }

    @Test
    void weekendLeaveSkipsSaturdaysAndSundays() {
        // Mon..Fri = 5 collection days, the 6th is the next Monday
        assertEquals(MONDAY.plusDays(7), LoanScheduleCalculator.collectionEndDate(MONDAY, 6, LeaveType.WEEKEND));
        assertEquals(MONDAY.plusDays(11), LoanScheduleCalculator.collectionEndDate(MONDAY, 10, LeaveType.WEEKEND));
    }

    @Test
    void startDateOnALeaveDayIsSkipped() {
        LocalDate sunday = MONDAY.minusDays(1);
        assertEquals(MONDAY, LoanScheduleCalculator.collectionEndDate(sunday, 1, LeaveType.SUNDAY));
    }

    @Test
    void leaveDays() {
        LocalDate saturday = MONDAY.plusDays(5);
        assertTrue(LoanScheduleCalculator.isLeaveDay(saturday, LeaveType.WEEKEND));
        assertFalse(LoanScheduleCalculator.isLeaveDay(saturday, LeaveType.SUNDAY));
        assertFalse(LoanScheduleCalculator.isLeaveDay(saturday.plusDays(1), LeaveType.NONE));
    }

    @Test
    void rejectsZeroTenure() {
        assertThrows(IllegalArgumentException.class,
                () -> LoanScheduleCalculator.terms(BigDecimal.TEN, BigDecimal.ZERO, 0, MONDAY, LeaveType.NONE));
    }
}
