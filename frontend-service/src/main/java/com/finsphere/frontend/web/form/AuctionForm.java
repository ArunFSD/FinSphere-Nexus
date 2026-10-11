package com.finsphere.frontend.web.form;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Settle one month's auction. */
public record AuctionForm(@Nullable Integer monthlyCount, @Nullable Long winnerUserId,
        @Nullable BigDecimal bidAmount) {
}
