package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuctionRequest {

    @NotNull(
            message = "Plan ID is mandatory",
            groups = FirstOrder.class
    )
    private Long planId;

    @NotNull(
            message = "Monthly Count is mandatory",
            groups = FirstOrder.class
    )
    private Integer monthlyCount;

     /*
    @NotNull(
            message = "Payable amount is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Payable amount cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal payableAmount;
    */

    @NotNull(
            message = "Bid Amount is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Bid Amount cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal bidAmount;

    @NotNull(
            message = "Winner User is mandatory",
            groups = FirstOrder.class
    )
    private Long winnerUserId;
}