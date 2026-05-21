package com.finsphere.common.model.chit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChitPlanDTO {

    private Long id;
    private String name;
    private BigDecimal totalValue;
    private Integer durationMonths;
    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;
    private BigDecimal monthlyInstallment;
    private BigDecimal maxBidLimit;
    private Integer totalMembers;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private LocalDateTime updatedAt;
}
