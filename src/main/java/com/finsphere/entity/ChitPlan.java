package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "chit_plans")
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
    private Integer commissionPercentage;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "monthly_installment", nullable = false)
    private BigDecimal monthlyInstallment;

    @Column(name = "max_bid_limit", nullable = false)
    private BigDecimal maxBidLimit;

    @Column(name = "min_members", nullable = false)
    private Integer minMembers;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // --- ADDED MAPPINGS START ---

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChitMonthlyCycle> monthlyCycles;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChitEnrollment> enrollments;

    // --- ADDED MAPPINGS END ---

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.totalValue != null && this.commissionPercentage != null) {
            this.commissionAmount = this.totalValue
                    .multiply(BigDecimal.valueOf(this.commissionPercentage))
                    .divide(BigDecimal.valueOf(100));
        }
        if (this.startDate != null && this.durationMonths != null) {
            this.endDate = this.startDate.plusMonths(this.durationMonths);
        }
        this.updatedAt = LocalDateTime.now();
    }
}