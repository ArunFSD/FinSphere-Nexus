package com.finsphere.chit.repository.jpa;

import java.util.Collection;
import java.util.List;

import com.finsphere.chit.domain.ChitEnrollment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChitEnrollmentRepository extends JpaRepository<ChitEnrollment, Long> {

    boolean existsByPlanIdAndSlotNumberAndActiveTrue(Long planId, Integer slotNumber);

    /** Slots this member holds in the plan. */
    long countByPlanIdAndUserIdAndActiveTrue(Long planId, Long userId);

    @EntityGraph(attributePaths = "plan")
    List<ChitEnrollment> findByPlanIdAndActiveTrueOrderBySlotNumberAsc(Long planId);

    @EntityGraph(attributePaths = "plan")
    List<ChitEnrollment> findByUserIdAndActiveTrueOrderByIdDesc(Long userId);

    /** Filled slots for a page of plans in one grouped query (no N+1). */
    @Query("""
            select e.plan.id as planId, count(e) as filled
            from ChitEnrollment e
            where e.plan.id in :planIds and e.active = true
            group by e.plan.id
            """)
    List<PlanSlotCount> countActiveByPlanIds(@Param("planIds") Collection<Long> planIds);

    interface PlanSlotCount {

        Long getPlanId();

        long getFilled();
    }
}
