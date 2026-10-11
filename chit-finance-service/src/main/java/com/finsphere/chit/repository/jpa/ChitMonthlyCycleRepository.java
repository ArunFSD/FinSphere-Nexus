package com.finsphere.chit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.finsphere.chit.domain.ChitMonthlyCycle;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChitMonthlyCycleRepository extends JpaRepository<ChitMonthlyCycle, Long> {

    /** Loads the cycle with its plan in one query (backed by the unique (plan_id, monthly_count) index). */
    @EntityGraph(attributePaths = "plan")
    Optional<ChitMonthlyCycle> findByPlanIdAndMonthlyCount(Long planId, Integer monthlyCount);

    long countByPlanIdAndWinnerUserId(Long planId, Long winnerUserId);

    List<ChitMonthlyCycle> findByPlanIdOrderByMonthlyCountAsc(Long planId);

    /** Settled months for a page of plans in one grouped query (no N+1). */
    @Query("""
            select c.plan.id as planId, count(c) as settled
            from ChitMonthlyCycle c
            where c.plan.id in :planIds and c.cycleClosed = true
            group by c.plan.id
            """)
    List<PlanSettledCount> countSettledByPlanIds(@Param("planIds") Collection<Long> planIds);

    interface PlanSettledCount {

        Long getPlanId();

        long getSettled();
    }
}
