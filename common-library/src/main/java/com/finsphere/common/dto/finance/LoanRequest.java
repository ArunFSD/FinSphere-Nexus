package com.finsphere.common.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import com.finsphere.common.validation.ValidationGroups.ThirdOrder;

/**
 * Disburses a daily-collection loan. Interest is deducted up front, so the member receives
 * {@code principalAmount - interestDeduction} and repays {@code principalAmount} over {@code tenureDays}
 * collection days.
 *
 * @param userId            borrower
 * @param principalAmount   amount to be repaid
 * @param interestDeduction interest deducted at disbursal
 * @param tenureDays        number of collection days
 * @param startDate         first collection day
 * @param leaveType         days skipped for collection
 */
public record LoanRequest(
        @NotNull(message = "Choose a member", groups = FirstOrder.class)
        Long userId,

        @NotNull(message = "Enter the loan amount", groups = FirstOrder.class)
        @DecimalMin(value = "1.00", message = "The loan amount must be more than zero", groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The loan amount is too large", groups = ThirdOrder.class)
        BigDecimal principalAmount,

        @NotNull(message = "Enter the interest deducted up front (0 if none)", groups = FirstOrder.class)
        @DecimalMin(value = "0.00", message = "Interest cannot be negative", groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "Interest is too large", groups = ThirdOrder.class)
        BigDecimal interestDeduction,

        @NotNull(message = "Enter the number of collection days", groups = FirstOrder.class)
        @Min(value = 1, message = "Collect for at least 1 day", groups = SecondOrder.class)
        @Max(value = 3650, message = "Collection can run for at most 3650 days", groups = ThirdOrder.class)
        Integer tenureDays,

        @NotNull(message = "Enter the first collection date", groups = FirstOrder.class)
        LocalDate startDate,

        @NotNull(message = "Choose the collection days", groups = FirstOrder.class)
        LeaveType leaveType) {
}
