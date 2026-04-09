package com.finsphere.service;

import com.finsphere.dto.EnrollmentRequest;
import com.finsphere.entity.ChitEnrollment;
import com.finsphere.entity.ChitPlan;
import com.finsphere.repository.ChitEnrollmentRepository;
import com.finsphere.repository.ChitPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChitEnrollmentService {

    private final ChitEnrollmentRepository enrollmentRepository;
    private final ChitPlanRepository planRepository;

    @Transactional
    public ChitEnrollment enrollUser(EnrollmentRequest request) {
        // 1. Validate Plan exists
        ChitPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + request.getPlanId()));

        // 2. Validate Slot Number isn't higher than Plan duration
        if (request.getSlotNumber() > plan.getDurationMonths()) {
            throw new RuntimeException("Invalid slot number for this plan");
        }

        // 3. Check if slot is already taken (Database UNIQUE constraint handles this too,
        // but it's better to check here for a clean error message)
        boolean isSlotTaken = enrollmentRepository.existsByPlanIdAndSlotNumber(
                request.getPlanId(), request.getSlotNumber());

        if (isSlotTaken) {
            throw new RuntimeException("Slot " + request.getSlotNumber() + " is already occupied.");
        }

        // 4. Create Enrollment
        ChitEnrollment enrollment = ChitEnrollment.builder()
                .plan(plan)
                .userId(request.getUserId())
                .slotNumber(request.getSlotNumber())
                .isActive(true)
                .build();

        return enrollmentRepository.save(enrollment);
    }
}
