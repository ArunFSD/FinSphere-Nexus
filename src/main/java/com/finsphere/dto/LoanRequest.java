package com.finsphere.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanRequest {
    private Long userId;
    private BigDecimal principalAmount;   // e.g., 10000
    private BigDecimal interestDeduction; // e.g., 1000
    private Integer tenureDays;           // e.g., 100
    private LocalDate startDate;          // When the daily collection begins
    /**
     * Values:
     * "NONE" (All 7 days)
     * "SUNDAY" (6 days a week)
     * "WEEKEND" (Sat & Sun leave - 5 days a week)
     */
    private String leaveType;
}
