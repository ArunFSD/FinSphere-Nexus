package com.finsphere.service.chit;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.security.SecurityUtils; // Import our new utility
import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.chit.ChitMonthlyCycle;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.repository.jpa.ChitMonthlyCycleRepository;
import com.finsphere.repository.jpa.ChitPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final SecurityUtils securityUtils; // Injecting the utility

    @Transactional
    public ApiResponse<ChitPlan> createPlan(ChitPlanRequest request) {
        log.info(">>>> [CHIT_PLAN_CREATE_START] Name: {} | Value: {}", request.getName(), request.getTotalValue());

        try {
            // 1. Identify the Admin using Rich JWT Data
            Long adminId = securityUtils.getCurrentUserId();
            String adminName = securityUtils.getCurrentUserFullName();

            // 2. Build the Plan Entity
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
                    .createdBy(adminId)
                    .build();

            // Note: endDate is calculated in the Entity @PrePersist method we wrote earlier
            ChitPlan savedPlan = planRepository.save(plan);
            log.info("<<<< [CHIT_PLAN_DB_SAVED] Plan ID: {} created by: {} (ID: {})",
                    savedPlan.getId(), adminName, adminId);

            // 3. Generate Monthly Cycles
            List<ChitMonthlyCycle> cycles = new ArrayList<>();
            for (int i = 1; i <= savedPlan.getDurationMonths(); i++) {
                cycles.add(ChitMonthlyCycle.builder()
                        .plan(savedPlan)
                        .monthlyCount(i)
                        .paymentWindowStart(savedPlan.getStartDate().plusMonths(i - 1))
                        .paymentWindowDeadline(savedPlan.getStartDate().plusMonths(i - 1).plusDays(request.getPaymentDuration()))
                        .monthlyPayableAmount(savedPlan.getMonthlyInstallment())
                        .isCycleClosed(false)
                        .auctionWinnerBid(BigDecimal.ZERO)
                        .dividendPerMember(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            cycleRepository.saveAll(cycles);
            log.info("<<<< [CHIT_CYCLES_GENERATED] {} months initialized for Plan ID: {}",
                    cycles.size(), savedPlan.getId());

            return ApiResponse.<ChitPlan>builder()
                    .success(true)
                    .status(HttpStatus.CREATED.value())
                    .message("Plan created successfully by " + adminName)
                    .data(savedPlan)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("!!!! [CHIT_PLAN_CREATE_ERROR] Failed: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<ChitPlan>> getAllActivePlans(Pageable pageable) {
        log.info(">>>> [CHIT_PLAN_FETCH_ACTIVE] Paginated request by: {}", securityUtils.getCurrentUserFullName());
        Page<ChitPlan> plans = planRepository.findByIsActiveTrue(pageable);
        return ApiResponse.<Page<ChitPlan>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Retrieved page " + plans.getNumber() + " of active plans")
                .data(plans)
                .timestamp(LocalDateTime.now())
                .build();
    }
}