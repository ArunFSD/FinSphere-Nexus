package com.finsphere.repository;

import com.finsphere.entity.ChitEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChitEnrollmentRepository extends JpaRepository<ChitEnrollment, Long> {

    boolean existsByPlanIdAndSlotNumber(Long planId, Integer slotNumber);

    // Count how many slots this user holds in this specific plan
    long countByPlanIdAndUserId(Long planId, Long userId);

}
