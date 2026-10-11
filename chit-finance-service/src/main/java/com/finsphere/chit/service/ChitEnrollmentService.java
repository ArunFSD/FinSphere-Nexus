package com.finsphere.chit.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.domain.ChitEnrollment;
import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.chit.domain.UserMirror;
import com.finsphere.chit.mapper.ChitEnrollmentMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.common.audit.Auditable;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.security.SecurityUtils;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enrolment of members into chit slots.
 */
@Service
public class ChitEnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(ChitEnrollmentService.class);

    private static final String SLOT_TAKEN = "This slot is already taken";

    private final ChitEnrollmentRepository enrollments;

    private final ChitPlanService planService;

    private final ChitEnrollmentMapper mapper;

    private final UserDirectoryService directory;

    private final Clock clock;

    public ChitEnrollmentService(ChitEnrollmentRepository enrollments, ChitPlanService planService,
            ChitEnrollmentMapper mapper, UserDirectoryService directory, Clock clock) {
        this.enrollments = enrollments;
        this.planService = planService;
        this.mapper = mapper;
        this.directory = directory;
        this.clock = clock;
    }

    /**
     * Customers may enrol only themselves; administrators may enrol any active member. The slot must exist in the
     * plan and be free (a partial unique index also guards against two concurrent enrolments).
     */
    @Transactional
    @Auditable(action = "CHIT_MEMBER_ENROLLED", resource = "CHIT_ENROLLMENT", resourceId = "#result.id")
    public ChitEnrollmentResponse enroll(EnrollmentRequest request) {
        if (!SecurityUtils.canAccessUser(request.userId())) {
            throw DomainException.forbidden("You can only enrol yourself into a scheme");
        }
        ChitPlan plan = this.planService.requirePlan(request.planId());
        if (!plan.isActive()) {
            throw DomainException.conflict("This scheme is closed to new members.", "planId",
                    "This scheme no longer accepts members");
        }
        if (!plan.isValidSlot(request.slotNumber())) {
            throw DomainException.badRequest("That slot is not part of this scheme.", "slotNumber",
                    "Slot must be between 1 and " + plan.getTotalMembers());
        }
        UserMirror member = this.directory.requireActiveMember(request.userId());
        if (this.enrollments.existsByPlanIdAndSlotNumberAndActiveTrue(plan.getId(), request.slotNumber())) {
            throw DomainException.conflict("That slot is already taken.", "slotNumber", SLOT_TAKEN);
        }
        ChitEnrollment saved;
        try {
            saved = this.enrollments.saveAndFlush(
                    new ChitEnrollment(plan, request.userId(), request.slotNumber(), this.clock.instant()));
        }
        catch (DataIntegrityViolationException ex) {
            throw DomainException.conflict("That slot is already taken.", "slotNumber", SLOT_TAKEN);
        }
        log.info("User {} enrolled in plan {} at slot {}", request.userId(), plan.getId(), request.slotNumber());
        return this.mapper.toResponse(saved, member.getFullName());
    }

    @Transactional(readOnly = true)
    public List<ChitEnrollmentResponse> enrollmentsOfPlan(Long planId) {
        this.planService.requirePlan(planId);
        return withNames(this.enrollments.findByPlanIdAndActiveTrueOrderBySlotNumberAsc(planId));
    }

    @Transactional(readOnly = true)
    public List<ChitEnrollmentResponse> enrollmentsOfUser(Long userId) {
        return withNames(this.enrollments.findByUserIdAndActiveTrueOrderByIdDesc(userId));
    }

    private List<ChitEnrollmentResponse> withNames(List<ChitEnrollment> list) {
        Map<Long, String> names = this.directory
            .namesOf(list.stream().map(ChitEnrollment::getUserId).collect(Collectors.toSet()));
        return list.stream()
            .map((enrollment) -> this.mapper.toResponse(enrollment, names.get(enrollment.getUserId())))
            .toList();
    }
}
