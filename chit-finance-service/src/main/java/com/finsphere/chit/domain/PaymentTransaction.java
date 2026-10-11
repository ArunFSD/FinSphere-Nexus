package com.finsphere.chit.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionType;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * A collected payment in the log book (MongoDB collection {@code transactions}). Append-only.
 * Amounts are stored as Decimal128, so they keep their precision and can be summed in queries.
 */
@Document(collection = "transactions")
@CompoundIndex(name = "user_tx_query", def = "{'user_id': 1, 'payment_date': -1}")
@CompoundIndex(name = "target_tx_query", def = "{'target_id': 1, 'transaction_type': 1}")
public class PaymentTransaction {

    @Id
    private @Nullable String id;

    @Field("user_id")
    private Long userId;

    @Field("transaction_type")
    private TransactionType transactionType;

    @Field(name = "amount", targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @Field("payment_date")
    private LocalDate paymentDate;

    @Field("payment_mode")
    private PaymentMode paymentMode;

    @Field("received_by_info")
    private String receivedByInfo;

    @Field("target_id")
    private Long targetId;

    @Field("remarks")
    private @Nullable String remarks;

    @Field("correlation_id")
    private @Nullable String correlationId;

    @CreatedBy
    @Field("created_by")
    private @Nullable Long createdBy;

    @CreatedDate
    @Field("created_at")
    private @Nullable Instant createdAt;

    public @Nullable String getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TransactionType getTransactionType() {
        return this.transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return this.paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMode getPaymentMode() {
        return this.paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getReceivedByInfo() {
        return this.receivedByInfo;
    }

    public void setReceivedByInfo(String receivedByInfo) {
        this.receivedByInfo = (receivedByInfo != null) ? receivedByInfo.trim() : null;
    }

    public Long getTargetId() {
        return this.targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public @Nullable String getRemarks() {
        return this.remarks;
    }

    public void setRemarks(@Nullable String remarks) {
        this.remarks = (remarks == null || remarks.isBlank()) ? null : remarks.trim();
    }

    public @Nullable String getCorrelationId() {
        return this.correlationId;
    }

    public void setCorrelationId(@Nullable String correlationId) {
        this.correlationId = correlationId;
    }

    public @Nullable Long getCreatedBy() {
        return this.createdBy;
    }

    public @Nullable Instant getCreatedAt() {
        return this.createdAt;
    }
}
