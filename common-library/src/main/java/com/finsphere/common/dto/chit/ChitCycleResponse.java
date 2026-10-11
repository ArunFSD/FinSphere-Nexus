package com.finsphere.common.dto.chit;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * One monthly cycle of a chit scheme (the monthly ledger line).
 *
 * @param id                    cycle id
 * @param planId                owning plan
 * @param monthlyCount          1-based month number
 * @param paymentWindowStart    first day members can pay
 * @param paymentWindowDeadline last day members can pay
 * @param auctionWinnerBid      winning bid (discount) for this month
 * @param dividendPerMember     dividend credited to every member
 * @param monthlyPayableAmount  what each member pays this month after dividend
 * @param winnerUserId          auction winner, once settled
 * @param closed                whether the auction for this month is settled
 */
public record ChitCycleResponse(
        Long id,
        Long planId,
        Integer monthlyCount,
        LocalDate paymentWindowStart,
        LocalDate paymentWindowDeadline,
        BigDecimal auctionWinnerBid,
        BigDecimal dividendPerMember,
        BigDecimal monthlyPayableAmount,
        @Nullable Long winnerUserId,
        boolean closed) {
}
