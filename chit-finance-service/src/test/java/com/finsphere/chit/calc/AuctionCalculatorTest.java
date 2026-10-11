package com.finsphere.chit.calc;

import java.math.BigDecimal;

import com.finsphere.chit.calc.AuctionCalculator.AuctionOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionCalculatorTest {

    private static final BigDecimal VALUE = new BigDecimal("100000");

    private static final BigDecimal COMMISSION_PCT = new BigDecimal("5");

    private static final BigDecimal INSTALLMENT = new BigDecimal("5000");

    @Test
    void surplusAboveTheCommissionIsSharedByAllMembers() {
        AuctionOutcome outcome = AuctionCalculator.settle(VALUE, COMMISSION_PCT, 20, INSTALLMENT,
                new BigDecimal("12000"));

        assertEquals(new BigDecimal("5000.00"), outcome.commission());
        assertEquals(new BigDecimal("350.00"), outcome.dividendPerMember()); // (12000 - 5000) / 20
        assertEquals(new BigDecimal("4650.00"), outcome.payablePerMember());
        assertEquals(new BigDecimal("88000.00"), outcome.prizeMoney());
    }

    @Test
    void bidBelowTheCommissionPaysNoDividend() {
        AuctionOutcome outcome = AuctionCalculator.settle(VALUE, COMMISSION_PCT, 20, INSTALLMENT,
                new BigDecimal("3000"));

        assertEquals(new BigDecimal("0.00"), outcome.dividendPerMember());
        assertEquals(new BigDecimal("5000.00"), outcome.payablePerMember());
        assertEquals(new BigDecimal("97000.00"), outcome.prizeMoney());
    }

    @Test
    void payableNeverGoesNegative() {
        AuctionOutcome outcome = AuctionCalculator.settle(VALUE, BigDecimal.ZERO, 2, new BigDecimal("100"),
                new BigDecimal("90000"));

        assertEquals(new BigDecimal("45000.00"), outcome.dividendPerMember());
        assertEquals(new BigDecimal("0.00"), outcome.payablePerMember());
    }

    @Test
    void rejectsBidsOutsideTheChitValue() {
        assertThrows(IllegalArgumentException.class,
                () -> AuctionCalculator.settle(VALUE, COMMISSION_PCT, 20, INSTALLMENT, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class,
                () -> AuctionCalculator.settle(VALUE, COMMISSION_PCT, 20, INSTALLMENT, new BigDecimal("100000.01")));
        assertThrows(IllegalArgumentException.class,
                () -> AuctionCalculator.settle(VALUE, COMMISSION_PCT, 0, INSTALLMENT, BigDecimal.ONE));
    }
}
