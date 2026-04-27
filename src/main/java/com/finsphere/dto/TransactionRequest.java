package com.finsphere.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {
    private Long userId;
    private BigDecimal amount;
    private String transactionType; // "CHIT_PAYMENT" or "LOAN_PAYMENT"
    private Long targetId;          // Loan ID or Monthly Cycle ID
    private String paymentMode;     // "CASH", "UPI", etc.
    private String receivedByInfo;  // e.g., "Arun - Cash", "PhonePe Ref: 12345"
    private String remarks;         // e.g., "Partial payment for month 4"
    private LocalDate paymentDate;
}
