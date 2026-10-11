package com.finsphere.common.format;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indian rupee amounts as people write them: {@code ₹1,00,000} (lakh/crore digit grouping, which the JDK's number
 * formats do not produce), with paise only when there are any.
 */
public final class Rupees {

    private Rupees() {
    }

    public static String format(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        String sign = (scaled.signum() < 0) ? "−" : "";
        String plain = scaled.abs().toPlainString();
        int dot = plain.indexOf('.');
        String fraction = plain.substring(dot + 1);
        return sign + "₹" + group(plain.substring(0, dot)) + ("00".equals(fraction) ? "" : "." + fraction);
    }

    /** Groups the integer digits 3 then 2 by 2: {@code 12345678 -> 1,23,45,678}. */
    public static String group(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        String lastThree = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder grouped = new StringBuilder();
        int index = rest.length() % 2;
        if (index > 0) {
            grouped.append(rest, 0, index);
        }
        for (; index < rest.length(); index += 2) {
            if (!grouped.isEmpty()) {
                grouped.append(',');
            }
            grouped.append(rest, index, index + 2);
        }
        return grouped + "," + lastThree;
    }
}
