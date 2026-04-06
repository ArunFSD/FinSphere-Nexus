package com.finsphere.service;

import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.ChitMonthlyCycle;
import com.finsphere.entity.ChitPlan;
import com.finsphere.repository.ChitMonthlyCycleRepository;
import com.finsphere.repository.ChitPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChitPlanService {

    private final ChitPlanRepository planRepository;
    private final ChitMonthlyCycleRepository cycleRepository;

    @Transactional
    public ChitPlan createPlan(ChitPlanRequest request) {
        // 1. Save the Plan
        ChitPlan plan = ChitPlan.builder()
                .name(request.getName())
                .totalValue(request.getTotalValue())
                .durationMonths(request.getDurationMonths())
                .commissionPercentage(request.getCommissionPercentage())
                .monthlyInstallment(request.getMonthlyInstallment())
                .maxBidLimit(request.getMaxBidLimit())
                .minMembers(request.getMinMembers())
                .startDate(request.getStartDate())
                .isActive(true)
                .build();

        ChitPlan savedPlan = planRepository.save(plan);

        // 2. Generate Monthly Cycles
        List<ChitMonthlyCycle> cycles = new ArrayList<>();
        for (int i = 1; i <= savedPlan.getDurationMonths(); i++) {
            BigDecimal payableForThisMonth;

            if (i == 2) {
                payableForThisMonth = savedPlan.getMonthlyInstallment();
            } else {
                payableForThisMonth = BigDecimal.ZERO;
            }

            cycles.add(ChitMonthlyCycle.builder()
                    .plan(savedPlan)
                    .monthlyCount(i)
                    .paymentWindowStart(savedPlan.getStartDate().plusMonths(i - 1))
                    .paymentWindowDeadline(savedPlan.getStartDate().plusMonths(i - 1).plusDays(10))
                    .monthlyPayableAmount(payableForThisMonth)
                    .isCycleClosed(false)
                    .build());
        }
        cycleRepository.saveAll(cycles);

        return savedPlan;
    }

    /**
     * Retrieves all plans that are currently marked as active.
     * Uses readOnly transaction for better performance with PostgreSQL.
     */
    @Transactional(readOnly = true)
    public List<ChitPlan> getAllActivePlans() {
        return planRepository.findByIsActiveTrue();
    }
}
