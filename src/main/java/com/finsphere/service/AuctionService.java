package com.finsphere.service;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.dto.AuctionRequest;
import com.finsphere.entity.ChitMonthlyCycle;
import com.finsphere.entity.ChitPlan;
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

    @Transactional
    public ApiResponse<ChitMonthlyCycle> processAuction(Long planId, Integer monthCount, AuctionRequest request) {
        log.info(">>>> [AUCTION_START] Plan: {} | Month: {} | Bid: {}", planId, monthCount, request.getBidAmount());

        // 1. Fetch the specific cycle
        ChitMonthlyCycle cycle = cycleRepository.findByPlanIdAndMonthlyCount(planId, monthCount)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Cycle not found",
                        "auction",
                        "Invalid month or plan")
                );

        if (cycle.getIsCycleClosed()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Cycle Closed",
                    "auction",
                    "This month is already settled"
            );
        }

        ChitPlan plan = cycle.getPlan();

        // 2. INDUSTRIAL MATH: Dividend Calculation
        // Surplus = Bid Amount - Company Commission (3% of Total Value)
        BigDecimal commission = plan.getTotalValue()
                .multiply(plan.getCommissionPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalDividendSurplus = request.getBidAmount().subtract(commission);

        // Dividend per Member = Surplus / Total Members
        BigDecimal dividendPerMember = totalDividendSurplus
                .divide(BigDecimal.valueOf(plan.getTotalMembers()), 2, RoundingMode.HALF_UP);

        // New Monthly Payable = Base Installment - Dividend
        BigDecimal actualPayable = plan.getMonthlyInstallment().subtract(dividendPerMember);

        // 3. Update Cycle State
        cycle.setAuctionWinnerBid(request.getBidAmount());
        cycle.setWinnerUserId(request.getWinnerUserId());
        cycle.setDividendPerMember(dividendPerMember);
        cycle.setMonthlyPayableAmount(actualPayable);
        cycle.setIsCycleClosed(true);

        ChitMonthlyCycle updatedCycle = cycleRepository.save(cycle);

        log.info("<<<< [AUCTION_SUCCESS] Winner: {} | Dividend: {} | New Payable: {}",
                request.getWinnerUserId(), dividendPerMember, actualPayable);

        return ApiResponse.<ChitMonthlyCycle>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Auction processed. Members pay ₹" + actualPayable + " this month.")
                .data(updatedCycle)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
