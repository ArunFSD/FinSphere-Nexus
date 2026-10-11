package com.finsphere.common.dto.chit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * A chit scheme as shown to clients.
 *
 * @param id                   plan id
 * @param name                 scheme name
 * @param totalValue           chit value (pot)
 * @param durationMonths       number of monthly cycles
 * @param commissionPercentage organiser commission in percent
 * @param commissionAmount     organiser commission per cycle
 * @param monthlyInstallment   base monthly installment per member
 * @param maxBidLimit          highest discount a member may bid in an auction
 * @param totalMembers         number of slots
 * @param filledSlots          slots currently taken
 * @param settledMonths        months whose auction has been settled
 * @param paymentWindowDays    days each monthly payment window stays open
 * @param startDate            first cycle start
 * @param endDate              scheme end
 * @param active               whether the scheme is running
 * @param updatedAt            last modification time
 */
public record ChitPlanResponse(
        Long id,
        String name,
        BigDecimal totalValue,
        Integer durationMonths,
        BigDecimal commissionPercentage,
        @Nullable BigDecimal commissionAmount,
        BigDecimal monthlyInstallment,
        BigDecimal maxBidLimit,
        Integer totalMembers,
        long filledSlots,
        long settledMonths,
        @Nullable Integer paymentWindowDays,
        LocalDate startDate,
        @Nullable LocalDate endDate,
        boolean active,
        @Nullable Instant updatedAt) {
}
