package com.finsphere.chit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import com.finsphere.chit.calc.ChitScheduleCalculator;
import com.finsphere.common.audit.AuditableEntity;

/**
 * A chit scheme. Table {@code fsn_finance.chit_plans}.
 */
@Entity
@Table(name = "chit_plans")
public class ChitPlan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "commission_percentage", nullable = false)
    private BigDecimal commissionPercentage;

    @Column(name = "commission_amount")
    private @Nullable BigDecimal commissionAmount;

    @Column(name = "monthly_installment", nullable = false)
    private BigDecimal monthlyInstallment;

    @Column(name = "max_bid_limit", nullable = false)
    private BigDecimal maxBidLimit;

    @Column(name = "total_members", nullable = false)
    private Integer totalMembers;

    @Column(name = "payment_window_days")
    private @Nullable Integer paymentWindowDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private @Nullable LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "plan")
    @OrderBy("monthlyCount ASC")
    private List<ChitMonthlyCycle> cycles = new ArrayList<>();

    @OneToMany(mappedBy = "plan")
    private List<ChitEnrollment> enrollments = new ArrayList<>();

    /** Commission amount and end date are always derived from the inputs, never trusted from clients. */
    @PrePersist
    @PreUpdate
    public void deriveValues() {
        if (this.totalValue != null && this.commissionPercentage != null) {
            this.commissionAmount = ChitScheduleCalculator.commissionAmount(this.totalValue,
                    this.commissionPercentage);
        }
        if (this.startDate != null && this.durationMonths != null) {
            this.endDate = ChitScheduleCalculator.endDate(this.startDate, this.durationMonths);
        }
    }

    public boolean isValidSlot(int slotNumber) {
        return slotNumber >= 1 && slotNumber <= this.totalMembers;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = (name != null) ? name.trim() : null;
    }

    public BigDecimal getTotalValue() {
        return this.totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public Integer getDurationMonths() {
        return this.durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public BigDecimal getCommissionPercentage() {
        return this.commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public @Nullable BigDecimal getCommissionAmount() {
        return this.commissionAmount;
    }

    public BigDecimal getMonthlyInstallment() {
        return this.monthlyInstallment;
    }

    public void setMonthlyInstallment(BigDecimal monthlyInstallment) {
        this.monthlyInstallment = monthlyInstallment;
    }

    public BigDecimal getMaxBidLimit() {
        return this.maxBidLimit;
    }

    public void setMaxBidLimit(BigDecimal maxBidLimit) {
        this.maxBidLimit = maxBidLimit;
    }

    public Integer getTotalMembers() {
        return this.totalMembers;
    }

    public void setTotalMembers(Integer totalMembers) {
        this.totalMembers = totalMembers;
    }

    public @Nullable Integer getPaymentWindowDays() {
        return this.paymentWindowDays;
    }

    public void setPaymentWindowDays(@Nullable Integer paymentWindowDays) {
        this.paymentWindowDays = paymentWindowDays;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public @Nullable LocalDate getEndDate() {
        return this.endDate;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ChitMonthlyCycle> getCycles() {
        return this.cycles;
    }

    public List<ChitEnrollment> getEnrollments() {
        return this.enrollments;
    }
}
