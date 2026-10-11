package com.finsphere.common.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * A disbursed loan.
 *
 * @param id                loan id
 * @param userId            borrower
 * @param memberName        borrower name, when known to the finance service
 * @param principalAmount   amount to be repaid
 * @param interestDeduction interest deducted at disbursal
 * @param disbursedAmount   cash handed to the borrower
 * @param dailyInstallment  amount collected per collection day
 * @param tenureDays        number of collection days
 * @param leaveType         days skipped for collection
 * @param amountGivenDate   disbursal date
 * @param startDate         first collection day
 * @param endDate           last collection day (leave days skipped)
 * @param active            whether the loan is open
 * @param createdAt         when the loan was recorded
 */
public record LoanResponse(
        Long id,
        Long userId,
        @Nullable String memberName,
        BigDecimal principalAmount,
        @Nullable BigDecimal interestDeduction,
        BigDecimal disbursedAmount,
        BigDecimal dailyInstallment,
        Integer tenureDays,
        LeaveType leaveType,
        LocalDate amountGivenDate,
        LocalDate startDate,
        @Nullable LocalDate endDate,
        boolean active,
        @Nullable Instant createdAt) {
}
