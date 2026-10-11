package com.finsphere.chit.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

import com.finsphere.common.dto.finance.LeaveType;

/**
 * Pure daily-collection loan arithmetic.
 */
public final class LoanScheduleCalculator {

    private LoanScheduleCalculator() {
    }

    /**
     * @param disbursedAmount  cash handed over (principal minus up-front interest)
     * @param dailyInstallment collected on each collection day
     * @param endDate          last collection day, leave days skipped
     */
    public record LoanTerms(BigDecimal disbursedAmount, BigDecimal dailyInstallment, LocalDate endDate) {
    }

    public static LoanTerms terms(BigDecimal principal, BigDecimal interestDeduction, int tenureDays,
            LocalDate startDate, LeaveType leaveType) {
        if (tenureDays < 1) {
            throw new IllegalArgumentException("tenureDays must be at least 1");
        }
        BigDecimal disbursed = principal.subtract(interestDeduction).setScale(2, RoundingMode.HALF_UP);
        BigDecimal daily = principal.divide(BigDecimal.valueOf(tenureDays), 2, RoundingMode.HALF_UP);
        return new LoanTerms(disbursed, daily, collectionEndDate(startDate, tenureDays, leaveType));
    }

    /**
     * The date of the {@code installments}-th collection day, counting from {@code startDate} and skipping leave
     * days (a start date that falls on a leave day is skipped too).
     */
    public static LocalDate collectionEndDate(LocalDate startDate, int installments, LeaveType leaveType) {
        if (installments < 1) {
            throw new IllegalArgumentException("installments must be at least 1");
        }
        LocalDate day = startDate;
        int counted = 0;
        while (true) {
            if (!isLeaveDay(day, leaveType)) {
                counted++;
                if (counted == installments) {
                    return day;
                }
            }
            day = day.plusDays(1);
        }
    }

    public static boolean isLeaveDay(LocalDate day, LeaveType leaveType) {
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        return switch (leaveType) {
            case NONE -> false;
            case SUNDAY -> dayOfWeek == DayOfWeek.SUNDAY;
            case WEEKEND -> dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        };
    }
}
