package com.finsphere.common.format;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RupeesTest {

    @Test
    void groupsDigitsTheIndianWay() {
        assertEquals("999", Rupees.group("999"));
        assertEquals("1,000", Rupees.group("1000"));
        assertEquals("1,00,000", Rupees.group("100000"));
        assertEquals("12,34,567", Rupees.group("1234567"));
        assertEquals("1,23,45,678", Rupees.group("12345678"));
    }

    @Test
    void showsPaiseOnlyWhenThereAreAny() {
        assertEquals("₹1,00,000", Rupees.format(new BigDecimal("100000.00")));
        assertEquals("₹4,650.50", Rupees.format(new BigDecimal("4650.5")));
        assertEquals("₹0", Rupees.format(BigDecimal.ZERO));
        assertEquals("₹0.01", Rupees.format(new BigDecimal("0.005")));
        assertEquals("−₹1,500", Rupees.format(new BigDecimal("-1500")));
    }
}
