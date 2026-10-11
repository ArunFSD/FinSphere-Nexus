package com.finsphere.chit.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure auction settlement arithmetic.
 * <p>
 * The winning bid is the discount the winner gives up. The organiser keeps the commission out of it and the
 * surplus is shared equally by all members as a dividend, which reduces everybody's installment for the month.
 */
public final class AuctionCalculator {

    private AuctionCalculator() {
    }

    /**
     * @param commission        organiser commission for the month
     * @param dividendPerMember surplus shared by every member
     * @param payablePerMember  installment due this month after the dividend (never negative)
     * @param prizeMoney        paid out to the winner: chit value minus the bid
     */
    public record AuctionOutcome(BigDecimal commission, BigDecimal dividendPerMember, BigDecimal payablePerMember,
            BigDecimal prizeMoney) {
    }

    public static AuctionOutcome settle(BigDecimal totalValue, BigDecimal commissionPercentage, int totalMembers,
            BigDecimal monthlyInstallment, BigDecimal winningBid) {
        if (totalMembers < 1) {
            throw new IllegalArgumentException("totalMembers must be positive");
        }
        if (winningBid.signum() < 0 || winningBid.compareTo(totalValue) > 0) {
            throw new IllegalArgumentException("winningBid must be between 0 and the chit value");
        }
        BigDecimal commission = ChitScheduleCalculator.commissionAmount(totalValue, commissionPercentage);
        BigDecimal surplus = winningBid.subtract(commission).max(BigDecimal.ZERO);
        BigDecimal dividend = surplus.divide(BigDecimal.valueOf(totalMembers), 2, RoundingMode.HALF_UP);
        BigDecimal payable = monthlyInstallment.subtract(dividend)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal prize = totalValue.subtract(winningBid).setScale(2, RoundingMode.HALF_UP);
        return new AuctionOutcome(commission, dividend, payable, prize);
    }
}
