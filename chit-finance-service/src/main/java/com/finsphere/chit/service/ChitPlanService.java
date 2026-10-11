package com.finsphere.chit.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.calc.ChitScheduleCalculator;
import com.finsphere.chit.domain.ChitEnrollment;
import com.finsphere.chit.domain.ChitMonthlyCycle;
import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.chit.mapper.ChitCycleMapper;
import com.finsphere.chit.mapper.ChitEnrollmentMapper;
import com.finsphere.chit.mapper.ChitPlanMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository.PlanSlotCount;
import com.finsphere.chit.repository.jpa.ChitMonthlyCycleRepository;
import com.finsphere.chit.repository.jpa.ChitMonthlyCycleRepository.PlanSettledCount;
import com.finsphere.chit.repository.jpa.ChitPlanRepository;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.audit.Auditable;
import com.finsphere.common.dto.chit.ChitCycleResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chit scheme lifecycle: creation with its monthly ledger, listing and detail views.
 */
@Service
public class ChitPlanService {

    private static final Logger log = LoggerFactory.getLogger(ChitPlanService.class);

    private final ChitPlanRepository plans;

    private final ChitMonthlyCycleRepository cycles;

    private final ChitEnrollmentRepository enrollments;

    private final ChitPlanMapper planMapper;

    private final ChitCycleMapper cycleMapper;

    private final ChitEnrollmentMapper enrollmentMapper;

    private final UserDirectoryService directory;

    public ChitPlanService(ChitPlanRepository plans, ChitMonthlyCycleRepository cycles,
            ChitEnrollmentRepository enrollments, ChitPlanMapper planMapper, ChitCycleMapper cycleMapper,
            ChitEnrollmentMapper enrollmentMapper, UserDirectoryService directory) {
        this.plans = plans;
        this.cycles = cycles;
        this.enrollments = enrollments;
        this.planMapper = planMapper;
        this.cycleMapper = cycleMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.directory = directory;
    }

    @Transactional
    @Auditable(action = "CHIT_PLAN_CREATED", resource = "CHIT_PLAN", resourceId = "#result.id")
    public ChitPlanResponse createPlan(ChitPlanRequest request) {
        if (request.maxBidLimit().compareTo(request.totalValue()) > 0) {
            throw DomainException.badRequest("Some scheme details need changing.", "maxBidLimit",
                    "The bid limit cannot be more than the chit value");
        }
        ChitPlan plan = this.plans.save(this.planMapper.toEntity(request));
        List<ChitMonthlyCycle> ledger = ChitScheduleCalculator
            .schedule(plan.getStartDate(), plan.getDurationMonths(), request.paymentDuration())
            .stream()
            .map((window) -> new ChitMonthlyCycle(plan, window.monthNumber(), window.opensOn(), window.closesOn(),
                    plan.getMonthlyInstallment()))
            .toList();
        this.cycles.saveAll(ledger);
        log.info("Created chit plan {} '{}' with {} monthly cycles", plan.getId(), plan.getName(), ledger.size());
        return this.planMapper.toResponse(plan, 0, 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChitPlanResponse> activePlans(Pageable pageable) {
        Page<ChitPlan> page = this.plans.findByActiveTrue(pageable);
        Map<Long, Long> filled = filledSlots(page.getContent());
        Map<Long, Long> settled = settledMonths(page.getContent());
        return PageResponse.from(page, (plan) -> this.planMapper.toResponse(plan,
                filled.getOrDefault(plan.getId(), 0L), settled.getOrDefault(plan.getId(), 0L)));
    }

    /**
     * Plan, full monthly ledger and members. Administrators see every member; customers only their own slots.
     */
    @Transactional(readOnly = true)
    public ChitPlanDetailResponse planDetail(Long planId) {
        ChitPlan plan = requirePlan(planId);
        List<ChitCycleResponse> ledger = this.cycleMapper
            .toResponses(this.cycles.findByPlanIdOrderByMonthlyCountAsc(planId));
        long settledMonths = ledger.stream().filter(ChitCycleResponse::closed).count();
        List<ChitEnrollment> active = this.enrollments.findByPlanIdAndActiveTrueOrderBySlotNumberAsc(planId);

        AuthenticatedUser viewer = SecurityUtils.requireCurrentUser();
        List<ChitEnrollment> visible = viewer.isAdmin() ? active
                : active.stream().filter((enrollment) -> enrollment.getUserId().equals(viewer.userId())).toList();
        Map<Long, String> names = this.directory
            .namesOf(visible.stream().map(ChitEnrollment::getUserId).collect(Collectors.toSet()));
        List<ChitEnrollmentResponse> members = visible.stream()
            .map((enrollment) -> this.enrollmentMapper.toResponse(enrollment, names.get(enrollment.getUserId())))
            .toList();
        List<Integer> takenSlots = active.stream().map(ChitEnrollment::getSlotNumber).sorted().toList();
        return new ChitPlanDetailResponse(this.planMapper.toResponse(plan, active.size(), settledMonths), ledger,
                members, takenSlots);
    }

    public ChitPlan requirePlan(Long planId) {
        return this.plans.findById(planId)
            .orElseThrow(() -> DomainException.notFound("Scheme not found", "planId", "No scheme with id " + planId));
    }

    private Map<Long, Long> settledMonths(List<ChitPlan> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        return this.cycles.countSettledByPlanIds(page.stream().map(ChitPlan::getId).toList())
            .stream()
            .collect(Collectors.toMap(PlanSettledCount::getPlanId, PlanSettledCount::getSettled));
    }

    private Map<Long, Long> filledSlots(List<ChitPlan> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        return this.enrollments.countActiveByPlanIds(page.stream().map(ChitPlan::getId).toList())
            .stream()
            .collect(Collectors.toMap(PlanSlotCount::getPlanId, PlanSlotCount::getFilled));
    }
}
