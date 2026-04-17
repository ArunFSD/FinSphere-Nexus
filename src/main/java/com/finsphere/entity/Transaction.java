package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_tx_user_id", columnList = "user_id"),
                @Index(name = "idx_tx_target_lookup", columnList = "target_id, transaction_type"),
                @Index(name = "idx_tx_date", columnList = "payment_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // CHIT_PAYMENT or LOAN_PAYMENT

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "payment_date")
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Builder.Default
    @Column(name = "payment_mode")
    private String paymentMode = "CASH";

    @Column(name = "received_by_info")
    private String receivedByInfo; // Store the collector's name or UPI ref

    @Column(name = "target_id", nullable = false)
    private Long targetId; // ID of the Loan or the Chit Cycle

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "created_by")
    private Long createdBy; // Admin ID from Token
}