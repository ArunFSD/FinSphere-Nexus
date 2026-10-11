package com.finsphere.common.dto.chit;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import com.finsphere.common.validation.ValidationGroups.ThirdOrder;

/**
 * Settles one month's auction.
 *
 * @param planId       plan id
 * @param monthlyCount month number being auctioned
 * @param bidAmount    winning bid (discount given up by the winner), at most the plan's max bid limit
 * @param winnerUserId winning member
 */
public record AuctionRequest(
        @NotNull(message = "Choose a scheme", groups = FirstOrder.class)
        Long planId,

        @NotNull(message = "Choose the month", groups = FirstOrder.class)
        @Min(value = 1, message = "Month must be 1 or later", groups = SecondOrder.class)
        Integer monthlyCount,

        @NotNull(message = "Enter the winning bid", groups = FirstOrder.class)
        @DecimalMin(value = "0.00", message = "The bid cannot be negative", groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The bid is too large", groups = ThirdOrder.class)
        BigDecimal bidAmount,

        @NotNull(message = "Choose the winning member", groups = FirstOrder.class)
        Long winnerUserId) {
}
