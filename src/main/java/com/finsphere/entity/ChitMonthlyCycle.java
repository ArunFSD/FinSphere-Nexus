package com.finsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chit_monthly_cycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChitMonthlyCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ChitPlan plan;

    @Column(name = "monthly_count", nullable = false)
    private Integer monthlyCount;

    @Column(name = "payment_window_start", nullable = false)
    private LocalDate paymentWindowStart;

    @Column(name = "payment_window_deadline", nullable = false)
    private LocalDate paymentWindowDeadline;

    @Builder.Default
    @Column(name = "auction_winner_bid")
    private BigDecimal auctionWinnerBid = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "dividend_per_member")
    private BigDecimal dividendPerMember = BigDecimal.ZERO;

    @Column(name = "monthly_payable_amount", nullable = false)
    private BigDecimal monthlyPayableAmount;

    @Column(name = "winner_user_id")
    private Long winnerUserId;

    @Builder.Default
    @Column(name = "is_cycle_closed")
    private Boolean isCycleClosed = false;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}