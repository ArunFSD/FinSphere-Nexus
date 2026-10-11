package com.finsphere.chit.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.calc.AuctionCalculator;
import com.finsphere.chit.calc.AuctionCalculator.AuctionOutcome;
import com.finsphere.chit.domain.ChitMonthlyCycle;
import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.chit.mapper.ChitCycleMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.chit.repository.jpa.ChitMonthlyCycleRepository;
import com.finsphere.common.audit.Auditable;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.AuctionResultResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.format.Rupees;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles a month's auction: validates the winner and bid, computes commission, dividend and prize money, and
 * closes the cycle.
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final ChitMonthlyCycleRepository cycles;

    private final ChitEnrollmentRepository enrollments;

    private final ChitCycleMapper cycleMapper;

    public AuctionService(ChitMonthlyCycleRepository cycles, ChitEnrollmentRepository enrollments,
            ChitCycleMapper cycleMapper) {
        this.cycles = cycles;
        this.enrollments = enrollments;
        this.cycleMapper = cycleMapper;
    }

    @Transactional
    @Auditable(action = "CHIT_AUCTION_SETTLED", resource = "CHIT_CYCLE", resourceId = "#result.cycle.id")
    public AuctionResultResponse settle(AuctionRequest request) {
        ChitMonthlyCycle cycle = this.cycles.findByPlanIdAndMonthlyCount(request.planId(), request.monthlyCount())
            .orElseThrow(() -> DomainException.notFound("That month is not part of this scheme.", "monthlyCount",
                    "Scheme " + request.planId() + " has no month " + request.monthlyCount()));
        ChitPlan plan = cycle.getPlan();
        if (cycle.isCycleClosed()) {
            throw DomainException.conflict("This month is already settled.", "monthlyCount",
                    "This month is already settled");
        }

        BigDecimal bid = request.bidAmount();
        if (bid.compareTo(plan.getMaxBidLimit()) > 0 || bid.compareTo(plan.getTotalValue()) > 0) {
            throw DomainException.badRequest("The bid is above the scheme's limit.", "bidAmount",
                    "Bids in this scheme can be at most " + Rupees.format(plan.getMaxBidLimit()) + ".");
        }

        long slotsHeld = this.enrollments.countByPlanIdAndUserIdAndActiveTrue(request.planId(),
                request.winnerUserId());
        if (slotsHeld == 0) {
            throw DomainException.badRequest("That member is not in this scheme.", "winnerUserId",
                    "This member is not enrolled in the scheme");
        }
        long timesWon = this.cycles.countByPlanIdAndWinnerUserId(request.planId(), request.winnerUserId());
        if (timesWon >= slotsHeld) {
            throw DomainException.conflict("This member has already won for every slot they hold.", "winnerUserId",
                    "This member has already won for all " + slotsHeld + " slot(s) they hold");
        }

        AuctionOutcome outcome = AuctionCalculator.settle(plan.getTotalValue(), plan.getCommissionPercentage(),
                plan.getTotalMembers(), plan.getMonthlyInstallment(), bid);
        cycle.settle(request.winnerUserId(), bid, outcome.dividendPerMember(), outcome.payablePerMember());
        // Flush now so a concurrent settlement of the same month surfaces here as an optimistic-lock conflict (409)
        ChitMonthlyCycle saved = this.cycles.saveAndFlush(cycle);
        log.info("Auction settled: plan {} month {} winner {} bid {} prize {}", request.planId(),
                request.monthlyCount(), request.winnerUserId(), bid, outcome.prizeMoney());
        return new AuctionResultResponse(this.cycleMapper.toResponse(saved), outcome.prizeMoney(),
                outcome.commission(), timesWon + 1, slotsHeld);
    }
}
