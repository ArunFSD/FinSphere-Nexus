package com.finsphere.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "transactions")
@CompoundIndex(name = "user_tx_query", def = "{'user_id': 1, 'payment_date': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("transaction_type")
    private String transactionType;

    private BigDecimal amount;

    @Field("payment_date")
    private LocalDate paymentDate;

    @Field("payment_mode")
    private String paymentMode;

    @Field("received_by_info")
    private String receivedByInfo;

    @Field("target_id")
    private Long targetId;

    private String remarks;

    @Field("created_by")
    private Long createdBy;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt; // Automatically populated if Auditing is enabled

}