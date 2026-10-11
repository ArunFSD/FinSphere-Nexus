package com.finsphere.chit.calc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.finsphere.chit.calc.ChitScheduleCalculator.CycleWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChitScheduleCalculatorTest {

    @Test
    void scheduleHasOneWindowPerMonthStartingOnTheStartDate() {
        List<CycleWindow> windows = ChitScheduleCalculator.schedule(LocalDate.of(2026, 1, 31), 3, 10);

        assertEquals(3, windows.size());
        assertEquals(new CycleWindow(1, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 10)), windows.get(0));
        // month-end start dates are clamped by java.time, exactly like the 1.x ledger
        assertEquals(new CycleWindow(2, LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 10)), windows.get(1));
        assertEquals(new CycleWindow(3, LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 10)), windows.get(2));
    }

    @Test
    void endDateIsStartPlusDuration() {
        assertEquals(LocalDate.of(2027, 9, 15), ChitScheduleCalculator.endDate(LocalDate.of(2026, 1, 15), 20));
    }

    @Test
    void commissionIsAPercentageOfTheChitValueRoundedToPaise() {
        assertEquals(new BigDecimal("5000.00"),
                ChitScheduleCalculator.commissionAmount(new BigDecimal("100000"), new BigDecimal("5")));
        assertEquals(new BigDecimal("3333.33"),
                ChitScheduleCalculator.commissionAmount(new BigDecimal("100000"), new BigDecimal("3.333333")));
    }

    @Test
    void rejectsEmptySchedules() {
        assertThrows(IllegalArgumentException.class, () -> ChitScheduleCalculator.schedule(LocalDate.now(), 0, 10));
        assertThrows(IllegalArgumentException.class, () -> ChitScheduleCalculator.schedule(LocalDate.now(), 10, 0));
    }
}
