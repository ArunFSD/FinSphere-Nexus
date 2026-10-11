package com.finsphere.frontend.web.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.finsphere.common.dto.finance.LeaveType;
import com.finsphere.common.dto.finance.PaymentMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatsTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private static Formats at(String instant) {
        return new Formats(INDIA, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private final Formats fmt = at("2026-10-09T13:30:00Z");

    @Test
    void moneyUsesIndianGroupingAndHidesZeroPaise() {
        assertEquals("₹1,00,000", this.fmt.inr(new BigDecimal("100000.00")));
        assertEquals("₹4,650.50", this.fmt.inr(new BigDecimal("4650.5")));
        assertEquals("—", this.fmt.inr(null));
        assertEquals("100000", this.fmt.plain(new BigDecimal("100000.00")));
        assertEquals("2.5%", this.fmt.percent(new BigDecimal("2.50")));
    }

    @Test
    void datesAreShownInTheBusinessTimeZone() {
        assertEquals("9 Oct 2026", this.fmt.date(LocalDate.of(2026, 10, 9)));
        assertEquals("9 Oct", this.fmt.dayMonth(LocalDate.of(2026, 10, 9)));
        // 20:00 UTC on the 9th is already the 10th in India
        assertEquals("10 Oct 2026", this.fmt.day(Instant.parse("2026-10-09T20:00:00Z")));
        assertEquals("2026-10-09", this.fmt.isoDate(this.fmt.today()));
        assertEquals("Friday, 9 October 2026", this.fmt.todayLong());
    }

    @Test
    void greetingFollowsTheLocalTimeOfDay() {
        assertEquals("Good morning", at("2026-10-09T03:00:00Z").greeting());
        assertEquals("Good afternoon", at("2026-10-09T08:00:00Z").greeting());
        assertEquals("Good evening", this.fmt.greeting());
    }

    @Test
    void namesAndNumbers() {
        assertEquals("98765 43210", this.fmt.phone("9876543210"));
        assertEquals("+44 20 7946 0958", this.fmt.phone("+44 20 7946 0958"));
        assertEquals("LN", this.fmt.initials("Lakshmi Narayanan"));
        assertEquals("A", this.fmt.initials("arun"));
        assertEquals("?", this.fmt.initials(" "));
        assertEquals("Lakshmi", this.fmt.firstName("Lakshmi Narayanan"));
        assertEquals("1 slot", this.fmt.plural(1, "slot", "slots"));
        assertEquals("2 slots", this.fmt.plural(2, "slot", "slots"));
    }

    @Test
    void labels() {
        assertEquals("Administrator", this.fmt.role("ADMIN"));
        assertEquals("Member", this.fmt.role("CUSTOMER"));
        assertEquals("Sundays off", this.fmt.leave(LeaveType.SUNDAY));
        assertEquals("Bank transfer", this.fmt.mode(PaymentMode.BANK_TRANSFER));
    }
}
