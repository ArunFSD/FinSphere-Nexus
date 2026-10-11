package com.finsphere.frontend.web.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.dto.finance.LeaveType;
import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionType;
import com.finsphere.common.format.Rupees;

/**
 * Formatting used by the templates (exposed to every page as {@code fmt}). Money uses Indian digit grouping
 * (₹1,00,000), which the JDK's number formats do not produce.
 */
public final class Formats {

    private static final Locale INDIA = Locale.of("en", "IN");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM uuuu", INDIA);

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", INDIA);

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a", INDIA);

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", INDIA);

    private static final String MISSING = "—";

    private final ZoneId zone;

    private final Clock clock;

    public Formats(ZoneId zone, Clock clock) {
        this.zone = zone;
        this.clock = clock;
    }

    /** {@code ₹1,00,000} for whole amounts, {@code ₹1,00,000.50} otherwise. */
    public String inr(@Nullable BigDecimal amount) {
        return (amount == null) ? MISSING : Rupees.format(amount);
    }

    /** Value for an {@code <input type="number">}: no grouping, no currency sign. */
    public String plain(@Nullable BigDecimal amount) {
        return (amount == null) ? "" : amount.stripTrailingZeros().toPlainString();
    }

    public String percent(@Nullable BigDecimal value) {
        return (value == null) ? MISSING : value.stripTrailingZeros().toPlainString() + "%";
    }

    public String date(@Nullable LocalDate date) {
        return (date == null) ? MISSING : DATE.format(date);
    }

    public String dayMonth(@Nullable LocalDate date) {
        return (date == null) ? MISSING : DAY_MONTH.format(date);
    }

    /** The calendar day of an instant, in the business time zone. */
    public String day(@Nullable Instant instant) {
        return (instant == null) ? MISSING : DATE.format(instant.atZone(this.zone));
    }

    public String dateTime(@Nullable Instant instant) {
        return (instant == null) ? MISSING : DATE_TIME.format(instant.atZone(this.zone));
    }

    public String isoDate(@Nullable LocalDate date) {
        return (date == null) ? "" : date.toString();
    }

    public LocalDate today() {
        return LocalDate.now(this.clock.withZone(this.zone));
    }

    public String todayLong() {
        return LONG_DATE.format(today());
    }

    public String greeting() {
        LocalTime now = LocalTime.now(this.clock.withZone(this.zone));
        if (now.isBefore(LocalTime.NOON)) {
            return "Good morning";
        }
        return now.isBefore(LocalTime.of(17, 0)) ? "Good afternoon" : "Good evening";
    }

    /** {@code 98765 43210} for ten-digit mobile numbers, anything else unchanged. */
    public String phone(@Nullable String phone) {
        if (phone == null || phone.isBlank()) {
            return MISSING;
        }
        return (phone.length() == 10 && phone.chars().allMatch(Character::isDigit))
                ? phone.substring(0, 5) + " " + phone.substring(5) : phone;
    }

    /** Up to two initials, for the slot board and avatars. */
    public String initials(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0))) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.isEmpty() ? "?" : initials.toString();
    }

    public String firstName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+")[0];
    }

    public String plural(long count, String one, String many) {
        return count + " " + ((count == 1) ? one : many);
    }

    public String role(@Nullable String role) {
        if ("ADMIN".equals(role)) {
            return "Administrator";
        }
        return "CUSTOMER".equals(role) ? "Member" : MISSING;
    }

    public String leave(@Nullable LeaveType leaveType) {
        if (leaveType == null) {
            return MISSING;
        }
        return switch (leaveType) {
            case NONE -> "Every day";
            case SUNDAY -> "Sundays off";
            case WEEKEND -> "Weekends off";
        };
    }

    public String mode(@Nullable PaymentMode mode) {
        if (mode == null) {
            return MISSING;
        }
        return switch (mode) {
            case CASH -> "Cash";
            case UPI -> "UPI";
            case BANK_TRANSFER -> "Bank transfer";
        };
    }

    public String paymentFor(@Nullable TransactionType type) {
        if (type == null) {
            return MISSING;
        }
        return switch (type) {
            case CHIT_PAYMENT -> "Chit installment";
            case LOAN_PAYMENT -> "Loan installment";
        };
    }
}
