package com.finsphere.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AuctionRequest {
    private Long planId;
    private Integer monthlyCount;
    private BigDecimal bidAmount;
    private BigDecimal payableAmount;
    private Long winnerUserId;
}
