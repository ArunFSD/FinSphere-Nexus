package com.finsphere.chit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import com.finsphere.common.audit.AuditableEntity;

/**
 * One month of a chit scheme. Table {@code fsn_finance.chit_monthly_cycles}; unique per (plan, month).
 * Optimistic locking (inherited {@code version}) stops two administrators from settling the same auction.
 */
@Entity
@Table(name = "chit_monthly_cycles")
public class ChitMonthlyCycle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ChitPlan plan;

    @Column(name = "monthly_count", nullable = false)
    private Integer monthlyCount;

    @Column(name = "payment_window_start", nullable = false)
    private LocalDate paymentWindowStart;

    @Column(name = "payment_window_deadline", nullable = false)
    private LocalDate paymentWindowDeadline;

    @Column(name = "auction_winner_bid", nullable = false)
    private BigDecimal auctionWinnerBid = BigDecimal.ZERO;

    @Column(name = "dividend_per_member", nullable = false)
    private BigDecimal dividendPerMember = BigDecimal.ZERO;

    @Column(name = "monthly_payable_amount", nullable = false)
    private BigDecimal monthlyPayableAmount;

    @Column(name = "winner_user_id")
    private @Nullable Long winnerUserId;

    @Column(name = "is_cycle_closed", nullable = false)
    private boolean cycleClosed;

    protected ChitMonthlyCycle() {
        // for JPA
    }

    public ChitMonthlyCycle(ChitPlan plan, int monthlyCount, LocalDate paymentWindowStart,
            LocalDate paymentWindowDeadline, BigDecimal monthlyPayableAmount) {
        this.plan = plan;
        this.monthlyCount = monthlyCount;
        this.paymentWindowStart = paymentWindowStart;
        this.paymentWindowDeadline = paymentWindowDeadline;
        this.monthlyPayableAmount = monthlyPayableAmount;
    }

    /** Records the auction result and closes the month. */
    public void settle(Long winnerUserId, BigDecimal winningBid, BigDecimal dividendPerMember,
            BigDecimal payableAmount) {
        this.winnerUserId = winnerUserId;
        this.auctionWinnerBid = winningBid;
        this.dividendPerMember = dividendPerMember;
        this.monthlyPayableAmount = payableAmount;
        this.cycleClosed = true;
    }

    public Long getId() {
        return this.id;
    }

    public ChitPlan getPlan() {
        return this.plan;
    }

    /** Plan id without initialising the lazy plan proxy. */
    public Long getPlanId() {
        return this.plan.getId();
    }

    public Integer getMonthlyCount() {
        return this.monthlyCount;
    }

    public LocalDate getPaymentWindowStart() {
        return this.paymentWindowStart;
    }

    public LocalDate getPaymentWindowDeadline() {
        return this.paymentWindowDeadline;
    }

    public BigDecimal getAuctionWinnerBid() {
        return this.auctionWinnerBid;
    }

    public BigDecimal getDividendPerMember() {
        return this.dividendPerMember;
    }

    public BigDecimal getMonthlyPayableAmount() {
        return this.monthlyPayableAmount;
    }

    public @Nullable Long getWinnerUserId() {
        return this.winnerUserId;
    }

    public boolean isCycleClosed() {
        return this.cycleClosed;
    }
}
