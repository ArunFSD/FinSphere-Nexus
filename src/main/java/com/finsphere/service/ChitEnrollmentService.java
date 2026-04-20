package com.finsphere.service;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.entity.chit.ChitEnrollment;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.repository.ChitEnrollmentRepository;
import com.finsphere.repository.ChitPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChitEnrollmentService {

    private final ChitEnrollmentRepository enrollmentRepository;
    private final ChitPlanRepository planRepository;

    @Transactional
    public ApiResponse<Void> enrollUser(Long planId, Long userId, Integer slotNumber) {
        log.info(">>>> [CHIT_ENROLL_START] PlanID: {} | UserID: {} | Slot: {}", planId, userId, slotNumber);

        // 1. Verify Plan Exists
        ChitPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.NOT_FOUND,
                        "Plan not found",
                        "enrollment",
                        "Plan ID invalid")
                );

        // 2. Check if Slot is already taken
        if (enrollmentRepository.existsByPlanIdAndSlotNumber(planId, slotNumber)) {
            log.warn("!!!! [CHIT_ENROLL_FAIL] Slot {} already occupied in Plan {}", slotNumber, planId);
            throw new DomainException(HttpStatus.CONFLICT, "Slot Occupied", "slot", "This slot is already taken");
        }

        // 3. Save Enrollment
        ChitEnrollment enrollment = ChitEnrollment.builder()
                .plan(plan)
                .userId(userId)
                .slotNumber(slotNumber)
                .isActive(true)
                .joinedDate(LocalDateTime.now())
                .build();

        enrollmentRepository.save(enrollment);
        log.info("<<<< [CHIT_ENROLL_SUCCESS] User {} enrolled in Plan {} at Slot {}", userId, planId, slotNumber);

        return ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.CREATED.value())
                .message("Successfully enrolled in " + plan.getName())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
