package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "loans",
        indexes = {
                @Index(name = "idx_loan_user_id", columnList = "user_id"),
                @Index(name = "idx_loan_start_date", columnList = "start_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "disbursed_amount", nullable = false)
    private BigDecimal disbursedAmount;

    @Column(name = "daily_installment", nullable = false)
    private BigDecimal dailyInstallment;

    @Column(name = "tenure_days", nullable = false)
    private Integer tenureDays;

    @Column(name = "amount_given_date", nullable = false)
    private LocalDate amountGivenDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void calculateEndDate() {
        if (this.startDate != null && this.tenureDays != null) {
            this.endDate = this.startDate.plusDays(this.tenureDays);
        }
    }
}