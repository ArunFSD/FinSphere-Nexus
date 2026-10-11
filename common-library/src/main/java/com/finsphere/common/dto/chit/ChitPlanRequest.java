package com.finsphere.common.dto.chit;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import com.finsphere.common.validation.ValidationGroups.ThirdOrder;

/**
 * Creates a chit scheme. Monthly cycles are generated from {@code startDate}, {@code durationMonths} and
 * {@code paymentDuration} (days each month's payment window stays open).
 */
public record ChitPlanRequest(
        @NotBlank(message = "Enter a scheme name", groups = FirstOrder.class)
        @Size(max = 200, message = "Keep the scheme name under 200 characters", groups = SecondOrder.class)
        String name,

        @NotNull(message = "Enter the chit value", groups = FirstOrder.class)
        @DecimalMin(value = "1.00", message = "The chit value must be more than zero", groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The chit value is too large", groups = ThirdOrder.class)
        BigDecimal totalValue,

        @NotNull(message = "Enter the duration in months", groups = FirstOrder.class)
        @Min(value = 1, message = "Duration must be at least 1 month", groups = SecondOrder.class)
        @Max(value = 120, message = "Duration can be at most 120 months", groups = ThirdOrder.class)
        Integer durationMonths,

        @NotNull(message = "Enter the commission percentage", groups = FirstOrder.class)
        @DecimalMin(value = "0.00", message = "Commission cannot be negative", groups = SecondOrder.class)
        @DecimalMax(value = "100.00", message = "Commission cannot be more than 100%", groups = ThirdOrder.class)
        BigDecimal commissionPercentage,

        @NotNull(message = "Enter the monthly installment", groups = FirstOrder.class)
        @DecimalMin(value = "1.00", message = "The installment must be more than zero",
                groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The installment is too large", groups = ThirdOrder.class)
        BigDecimal monthlyInstallment,

        @NotNull(message = "Enter the highest bid allowed", groups = FirstOrder.class)
        @DecimalMin(value = "0.00", message = "The bid limit cannot be negative", groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The bid limit is too large", groups = ThirdOrder.class)
        BigDecimal maxBidLimit,

        @NotNull(message = "Enter the number of members", groups = FirstOrder.class)
        @Min(value = 2, message = "A scheme needs at least 2 members", groups = SecondOrder.class)
        @Max(value = 1000, message = "A scheme can have at most 1000 members", groups = ThirdOrder.class)
        Integer totalMembers,

        @NotNull(message = "Enter the start date", groups = FirstOrder.class)
        LocalDate startDate,

        @NotNull(message = "Enter how many days the payment window stays open", groups = FirstOrder.class)
        @Min(value = 1, message = "The payment window must be at least 1 day", groups = SecondOrder.class)
        @Max(value = 28, message = "The payment window can be at most 28 days", groups = ThirdOrder.class)
        Integer paymentDuration) {
}
