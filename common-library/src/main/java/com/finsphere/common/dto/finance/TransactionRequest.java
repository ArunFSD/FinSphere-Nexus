package com.finsphere.common.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import com.finsphere.common.validation.ValidationGroups.ThirdOrder;

/**
 * Records a collected payment in the transaction log book.
 *
 * @param userId          paying member
 * @param amount          amount received
 * @param transactionType chit installment or loan installment
 * @param targetId        plan id (chit) or loan id (loan)
 * @param paymentMode     cash, UPI or bank transfer
 * @param receivedByInfo  who collected the money
 * @param remarks         optional notes
 * @param paymentDate     date of payment (not in the future)
 */
public record TransactionRequest(
        @NotNull(message = "Choose a member", groups = FirstOrder.class)
        Long userId,

        @NotNull(message = "Enter the amount received", groups = FirstOrder.class)
        @DecimalMin(value = "0.01", message = "The amount must be more than zero",
                groups = SecondOrder.class)
        @Digits(integer = 13, fraction = 2, message = "The amount is too large", groups = ThirdOrder.class)
        BigDecimal amount,

        @NotNull(message = "Choose what the payment is for", groups = FirstOrder.class)
        TransactionType transactionType,

        @NotNull(message = "Choose the scheme or loan this payment is for", groups = FirstOrder.class)
        Long targetId,

        @NotNull(message = "Choose how the money was paid", groups = FirstOrder.class)
        PaymentMode paymentMode,

        @NotBlank(message = "Enter who collected the money", groups = FirstOrder.class)
        @Size(max = 250, message = "Keep this under 250 characters", groups = SecondOrder.class)
        String receivedByInfo,

        @Size(max = 500, message = "Keep remarks under 500 characters", groups = SecondOrder.class)
        String remarks,

        @NotNull(message = "Enter the payment date", groups = FirstOrder.class)
        @PastOrPresent(message = "The payment date cannot be in the future", groups = SecondOrder.class)
        LocalDate paymentDate) {
}
