package com.finsphere.common.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * A payment entry from the transaction log book.
 *
 * @param id              document id
 * @param userId          paying member
 * @param memberName      member name, when known to the finance service
 * @param transactionType chit installment or loan installment
 * @param amount          amount received
 * @param paymentDate     date of payment
 * @param paymentMode     cash, UPI or bank transfer
 * @param receivedByInfo  who collected the money
 * @param targetId        plan id or loan id
 * @param remarks         optional notes
 * @param createdBy       administrator who recorded it
 * @param createdAt       when it was recorded
 */
public record TransactionResponse(
        String id,
        Long userId,
        @Nullable String memberName,
        TransactionType transactionType,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMode paymentMode,
        String receivedByInfo,
        Long targetId,
        @Nullable String remarks,
        @Nullable Long createdBy,
        @Nullable Instant createdAt) {
}
