package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChitPlanRequest {

    @NotBlank(
            message = "Chit Plan Name is mandatory",
            groups = FirstOrder.class
    )
    @Size(
            max = 200,
            message = "Plan Name must not exceed 200 characters",
            groups = SecondOrder.class
    )
    private String name;

    @NotNull(
            message = "Total Value is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Total Value cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal totalValue;

    @NotNull(
            message = "Duration In Months is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 1,
            message = "Duration must be at least 1 month",
            groups = SecondOrder.class
    )
    private Integer durationMonths;

    @NotNull(
            message = "Commission is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Commission cannot be negative",
            groups = SecondOrder.class
    )
    @DecimalMax(value = "100.00",
            message = "Commission cannot exceed 100%",
            groups = ThirdOrder.class
    )
    private BigDecimal commissionPercentage;

    @NotNull(
            message = "Monthly Installment is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Monthly Installment cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal monthlyInstallment;

    @NotNull(
            message = "Max Bid Limit is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Max Bid Limit cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal maxBidLimit;

    @NotNull(
            message = "Total Members Count is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 2,
            message = "At least 2 members are required for a chit",
            groups = SecondOrder.class
    )
    private Integer totalMembers;

    @NotNull(
            message = "Start Date is mandatory",
            groups = FirstOrder.class
    )
    private LocalDate startDate;


    @NotNull(
            message = "Payment Duration is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 1,
            message = "Payment Duration must be at least 1",
            groups = SecondOrder.class
    )
    private Integer paymentDuration;
}