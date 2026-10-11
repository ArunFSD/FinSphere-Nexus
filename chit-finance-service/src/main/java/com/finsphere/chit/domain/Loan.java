package com.finsphere.chit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import com.finsphere.common.audit.AuditableEntity;
import com.finsphere.common.dto.finance.LeaveType;

/**
 * A daily-collection loan. Table {@code fsn_finance.loans}.
 */
@Entity
@Table(name = "loans")
public class Loan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "interest_deduction")
    private @Nullable BigDecimal interestDeduction;

    @Column(name = "disbursed_amount", nullable = false)
    private BigDecimal disbursedAmount;

    @Column(name = "daily_installment", nullable = false)
    private BigDecimal dailyInstallment;

    @Column(name = "tenure_days", nullable = false)
    private Integer tenureDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 10)
    private LeaveType leaveType = LeaveType.NONE;

    @Column(name = "amount_given_date", nullable = false)
    private LocalDate amountGivenDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private @Nullable LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getPrincipalAmount() {
        return this.principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public @Nullable BigDecimal getInterestDeduction() {
        return this.interestDeduction;
    }

    public void setInterestDeduction(@Nullable BigDecimal interestDeduction) {
        this.interestDeduction = interestDeduction;
    }

    public BigDecimal getDisbursedAmount() {
        return this.disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public BigDecimal getDailyInstallment() {
        return this.dailyInstallment;
    }

    public void setDailyInstallment(BigDecimal dailyInstallment) {
        this.dailyInstallment = dailyInstallment;
    }

    public Integer getTenureDays() {
        return this.tenureDays;
    }

    public void setTenureDays(Integer tenureDays) {
        this.tenureDays = tenureDays;
    }

    public LeaveType getLeaveType() {
        return this.leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getAmountGivenDate() {
        return this.amountGivenDate;
    }

    public void setAmountGivenDate(LocalDate amountGivenDate) {
        this.amountGivenDate = amountGivenDate;
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

    public void setEndDate(@Nullable LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
