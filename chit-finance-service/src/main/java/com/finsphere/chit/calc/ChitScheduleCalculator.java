package com.finsphere.chit.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure chit scheme arithmetic (no Spring, no database), unit tested on its own.
 */
public final class ChitScheduleCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ChitScheduleCalculator() {
    }

    /**
     * One payment window per month.
     *
     * @param monthNumber 1-based month
     * @param opensOn     first day members can pay
     * @param closesOn    last day members can pay
     */
    public record CycleWindow(int monthNumber, LocalDate opensOn, LocalDate closesOn) {
    }

    /**
     * Month {@code n} opens {@code n-1} months after the start date and stays open for {@code paymentWindowDays}.
     */
    public static List<CycleWindow> schedule(LocalDate startDate, int durationMonths, int paymentWindowDays) {
        if (durationMonths < 1) {
            throw new IllegalArgumentException("durationMonths must be at least 1");
        }
        if (paymentWindowDays < 1) {
            throw new IllegalArgumentException("paymentWindowDays must be at least 1");
        }
        List<CycleWindow> windows = new ArrayList<>(durationMonths);
        for (int month = 1; month <= durationMonths; month++) {
            LocalDate opensOn = startDate.plusMonths(month - 1L);
            windows.add(new CycleWindow(month, opensOn, opensOn.plusDays(paymentWindowDays)));
        }
        return List.copyOf(windows);
    }

    public static LocalDate endDate(LocalDate startDate, int durationMonths) {
        return startDate.plusMonths(durationMonths);
    }

    /** Organiser commission: total value x commission % / 100, rounded half-up to paise. */
    public static BigDecimal commissionAmount(BigDecimal totalValue, BigDecimal commissionPercentage) {
        return totalValue.multiply(commissionPercentage).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
