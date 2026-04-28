package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequest {

    @NotNull(
            message = "User is mandatory",
            groups = FirstOrder.class
    )
    private Long userId;

    @NotNull(
            message = "Amount is mandatory",
            groups = FirstOrder.class
    )
    @DecimalMin(
            value = "0.00",
            message = "Transaction Amount cannot be negative",
            groups = SecondOrder.class
    )
    private BigDecimal amount;

    @NotBlank(
            message = "Transaction Type is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^(CHIT_PAYMENT|LOAN_PAYMENT)$",
            message = "Type must be CHIT_PAYMENT or LOAN_PAYMENT",
            groups = SecondOrder.class
    )
    private String transactionType;

    @NotNull(
            message = "Plan is mandatory",
            groups = FirstOrder.class
    )
    private Long targetId;

    @NotBlank(
            message = "Payment Mode is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^(CASH|UPI|BANK_TRANSFER)$",
            message = "Mode must be CASH, UPI, or BANK_TRANSFER",
            groups = SecondOrder.class
    )
    private String paymentMode;

    @NotBlank(
            message = "Received By Info is mandatory",
            groups = FirstOrder.class
    )
    @Size(
            max = 250,
            message = "Received By Info must not exceed 200 characters",
            groups = SecondOrder.class
    )
    private String receivedByInfo;

    @Size(
            max = 500,
            message = "Remarks exceed 500 characters",
            groups = SecondOrder.class
    )
    private String remarks;

    @NotNull(
            message = "Payment Date is mandatory",
            groups = FirstOrder.class
    )
    @PastOrPresent(
            message = "Payment Date cannot be in the future",
            groups = SecondOrder.class
    )
    private LocalDate paymentDate;
}
