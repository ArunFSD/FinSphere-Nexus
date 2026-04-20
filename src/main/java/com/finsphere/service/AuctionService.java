package com.finsphere.service;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.dto.AuctionRequest;
import com.finsphere.entity.chit.ChitMonthlyCycle;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.repository.ChitEnrollmentRepository;
import com.finsphere.repository.ChitMonthlyCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final ChitMonthlyCycleRepository cycleRepository;
    private final ChitEnrollmentRepository enrollmentRepository;

    @Transactional
    public ApiResponse<ChitMonthlyCycle> processAuction(Long planId, Integer monthCount, AuctionRequest request) {

        log.info(">>>> [AUCTION_START] Plan: {} | Month: {} | Bid: {} | Winner: {}",
                planId, monthCount, request.getBidAmount(), request.getWinnerUserId());

        ChitMonthlyCycle cycle = cycleRepository.findByPlanIdAndMonthlyCount(planId, monthCount)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Cycle not found",
                        "auction",
                        "Invalid month or plan")
                );

        if (Boolean.TRUE.equals(cycle.getIsCycleClosed())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Cycle Closed",
                    "auction",
                    "This month is already settled"
            );
        }

        // 1. VALIDATION: How many slots does this user hold in this plan?
        long slotsOwned = enrollmentRepository.countByPlanIdAndUserId(planId, request.getWinnerUserId());
        if (slotsOwned == 0) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Winner",
                    "winner",
                    "User is not enrolled in this plan"
            );
        }

        // 2. VALIDATION: How many times has this user already won in this plan?
        long timesWon = cycleRepository.countByPlanIdAndWinnerUserId(planId, request.getWinnerUserId());

        // A user can only win as many times as the number of slots they hold
        if (timesWon >= slotsOwned) {
            log.warn("!!!! [AUCTION_FAIL] User {} attempted to win again but only owns {} slots",
                    request.getWinnerUserId(), slotsOwned);

            throw new DomainException(
                    HttpStatus.CONFLICT,
                    "Exhausted Wins",
                    "winner",
                    "User has already won for all " + slotsOwned + " slots they hold."
            );
        }

        ChitPlan plan = cycle.getPlan();

        // 3. INDUSTRIAL MATH: Dividend Calculation
        BigDecimal commission = plan.getTotalValue()
                .multiply(plan.getCommissionPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalDividendSurplus = request.getBidAmount().subtract(commission);

        BigDecimal dividendPerMember = totalDividendSurplus
                .divide(BigDecimal.valueOf(plan.getTotalMembers()), 2, RoundingMode.HALF_UP);

        BigDecimal actualPayable = plan.getMonthlyInstallment().subtract(dividendPerMember);

        // 4. Update Cycle State
        cycle.setAuctionWinnerBid(request.getBidAmount());
        cycle.setWinnerUserId(request.getWinnerUserId());
        cycle.setDividendPerMember(dividendPerMember);
        cycle.setMonthlyPayableAmount(actualPayable);
        cycle.setIsCycleClosed(true);

        ChitMonthlyCycle updatedCycle = cycleRepository.save(cycle);

        log.info("<<<< [AUCTION_SUCCESS] Plan: {} | Winner: {} (Win #{} of {}) | New Payable: {}",
                planId, request.getWinnerUserId(), timesWon + 1, slotsOwned, actualPayable);

        return ApiResponse.<ChitMonthlyCycle>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Auction processed. Prize Money: ₹" + plan.getTotalValue().subtract(request.getBidAmount()))
                .data(updatedCycle)
                .timestamp(LocalDateTime.now())
                .build();
    }
}