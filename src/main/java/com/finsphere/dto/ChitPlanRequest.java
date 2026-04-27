package com.finsphere.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChitPlanRequest {
    private String name;
    private BigDecimal totalValue;
    private Integer durationMonths;
    private BigDecimal commissionPercentage;
    private BigDecimal monthlyInstallment;
    private BigDecimal maxBidLimit;
    private Integer totalMembers;
    private LocalDate startDate;
    private Integer paymentDuration;
}
