package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "chit_plans") // UPDATED: Matches your SQL and YAML
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "commission_percentage", nullable = false)
    private BigDecimal commissionPercentage;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "monthly_installment", nullable = false)
    private BigDecimal monthlyInstallment;

    @Column(name = "max_bid_limit", nullable = false)
    private BigDecimal maxBidLimit;

    @Column(name = "total_members", nullable = false)
    private Integer totalMembers;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- RELATIONSHIPS (CRITICAL FOR JPA QUERIES) ---

    // This fixes the "UnknownPathException: Could not resolve attribute 'monthlyCycles'"
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChitMonthlyCycle> monthlyCycles;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChitEnrollment> enrollments;

    // --- AUDIT & FINANCIAL LOGIC ---

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        calculateFinancials();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateFinancials();
    }

    private void calculateFinancials() {
        if (this.totalValue != null && this.commissionPercentage != null) {
            // Formula: (Total Value * Commission %) / 100
            this.commissionAmount = this.totalValue
                    .multiply(this.commissionPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (this.startDate != null && this.durationMonths != null) {
            this.endDate = this.startDate.plusMonths(this.durationMonths);
        }
    }
}