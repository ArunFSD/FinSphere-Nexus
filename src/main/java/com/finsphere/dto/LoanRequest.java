package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanRequest {

    @NotNull(
            message = "User is mandatory",
            groups = FirstOrder.class
    )
    private Long userId;

    @NotNull(
            message = "Principal amount is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Principal amount  cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal principalAmount;

    @NotNull(
            message = "Interest deduction is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Interest deduction cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal interestDeduction;

    @NotNull(
            message = "Tenure days is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 1,
            message = "Tenure must be at least 1 day",
            groups = SecondOrder.class
    )
    private Integer tenureDays;

    @NotNull(
            message = "Start date is mandatory",
            groups = FirstOrder.class
    )
    private LocalDate startDate;

    @NotBlank(
            message = "Leave type is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^(NONE|SUNDAY|WEEKEND)$",
            message = "Leave type must be NONE, SUNDAY, or WEEKEND",
            groups = SecondOrder.class
    )
    private String leaveType;
}