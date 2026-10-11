package com.finsphere.frontend.web.form;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionType;

/**
 * Record a payment. {@code target} combines what the payment is for and its id ({@code CHIT_PAYMENT:5} or
 * {@code LOAN_PAYMENT:12}), so one list shows the member's schemes and loans together.
 */
public record PaymentForm(@Nullable Long userId, @Nullable String target, @Nullable BigDecimal amount,
        @Nullable LocalDate paymentDate, @Nullable PaymentMode paymentMode, @Nullable String receivedByInfo,
        @Nullable String remarks) {

    public static PaymentForm forMember(@Nullable Long userId, LocalDate today) {
        return new PaymentForm(userId, null, null, today, PaymentMode.CASH, "", "");
    }

    public static String target(TransactionType type, long id) {
        return type.name() + ":" + id;
    }

    /** Builds the API request, or returns {@code null} when {@code target} is not one of the offered choices. */
    public @Nullable TransactionRequest toRequest() {
        if (this.target == null) {
            return null;
        }
        int colon = this.target.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        try {
            TransactionType type = TransactionType.valueOf(this.target.substring(0, colon));
            long targetId = Long.parseLong(this.target.substring(colon + 1));
            return new TransactionRequest(this.userId, this.amount, type, targetId, this.paymentMode,
                    (this.receivedByInfo != null) ? this.receivedByInfo.trim() : null,
                    (this.remarks == null || this.remarks.isBlank()) ? null : this.remarks.trim(), this.paymentDate);
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
