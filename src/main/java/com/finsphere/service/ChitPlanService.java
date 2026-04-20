package com.finsphere.service;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.chit.ChitMonthlyCycle;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.repository.ChitMonthlyCycleRepository;
import com.finsphere.repository.ChitPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChitPlanService {

    private final ChitPlanRepository planRepository;
    private final ChitMonthlyCycleRepository cycleRepository;

    @Transactional
    public ApiResponse<ChitPlan> createPlan(ChitPlanRequest request) {
        log.info(">>>> [CHIT_PLAN_CREATE_START] Name: {} | Value: {}", request.getName(), request.getTotalValue());

        try {
            // 1. Identify the Admin
            String adminIdentifier = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // 2. Save the Plan
            ChitPlan plan = ChitPlan.builder()
                    .name(request.getName())
                    .totalValue(request.getTotalValue())
                    .durationMonths(request.getDurationMonths())
                    .commissionPercentage(request.getCommissionPercentage())
                    .monthlyInstallment(request.getMonthlyInstallment())
                    .maxBidLimit(request.getMaxBidLimit())
                    .totalMembers(request.getTotalMembers())
                    .startDate(request.getStartDate())
                    .isActive(true)
                    .build();

            ChitPlan savedPlan = planRepository.save(plan);
            log.info("<<<< [CHIT_PLAN_DB_SAVED] Plan ID: {} created by: {}", savedPlan.getId(), adminIdentifier);

            // 3. Generate Monthly Cycles
            List<ChitMonthlyCycle> cycles = new ArrayList<>();
            for (int i = 1; i <= savedPlan.getDurationMonths(); i++) {

                cycles.add(ChitMonthlyCycle.builder()
                        .plan(savedPlan)
                        .monthlyCount(i)
                        .paymentWindowStart(savedPlan.getStartDate().plusMonths(i - 1))
                        .paymentWindowDeadline(savedPlan.getStartDate().plusMonths(i - 1).plusDays(10))
                        // Initialized to full installment; this decreases after auctions
                        .monthlyPayableAmount(savedPlan.getMonthlyInstallment())
                        .isCycleClosed(false)
                        .auctionWinnerBid(BigDecimal.ZERO)
                        .dividendPerMember(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            cycleRepository.saveAll(cycles);
            log.info("<<<< [CHIT_CYCLES_GENERATED] {} months initialized for Plan ID: {}", cycles.size(), savedPlan.getId());

            return ApiResponse.<ChitPlan>builder()
                    .success(true)
                    .status(HttpStatus.CREATED.value())
                    .message("Plan and " + savedPlan.getDurationMonths() + " cycles created.")
                    .data(savedPlan)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("!!!! [CHIT_PLAN_CREATE_ERROR] Failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves all plans that are currently marked as active.
     * Uses readOnly transaction for better performance with PostgreSQL.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ChitPlan>> getAllActivePlans() {
        log.info(">>>> [CHIT_PLAN_FETCH_ACTIVE] Requesting list of available plans");

        List<ChitPlan> plans = planRepository.findByIsActiveTrue();

        return ApiResponse.<List<ChitPlan>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Successfully retrieved " + plans.size() + " active plans")
                .data(plans)
                .timestamp(LocalDateTime.now())
                .build();
    }
}