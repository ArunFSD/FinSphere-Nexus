package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChitPlanRequest {

    @NotBlank(
            message = "Chit Plan name is mandatory",
            groups = FirstOrder.class
    )
    @Size(
            max = 200,
            message = "Plan name must not exceed 200 characters",
            groups = SecondOrder.class
    )
    private String name;

    @NotNull(
            message = "Total value is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Total value cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal totalValue;

    @NotNull(
            message = "Duration in months is mandatory",
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
            message = "Monthly installment is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Monthly installment cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal monthlyInstallment;

    @NotNull(
            message = "Max bid limit is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Max bid limit cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal maxBidLimit;

    @NotNull(
            message = "Total members count is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 2,
            message = "At least 2 members are required for a chit",
            groups = SecondOrder.class
    )
    private Integer totalMembers;

    @NotNull(
            message = "Start date is mandatory",
            groups = FirstOrder.class
    )
    private LocalDate startDate;


    @NotNull(
            message = "Payment Duration is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 1,
            message = "Payment duration must be at least 1",
            groups = SecondOrder.class
    )
    private Integer paymentDuration;
}