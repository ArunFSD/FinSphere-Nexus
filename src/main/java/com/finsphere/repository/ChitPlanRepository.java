package com.finsphere.repository;

import com.finsphere.entity.ChitPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChitPlanRepository extends JpaRepository<ChitPlan, Long> {

    List<ChitPlan> findByIsActiveTrue();

    @Query("SELECT DISTINCT p FROM ChitPlan p LEFT JOIN FETCH p.monthlyCycles WHERE p.isActive = true")
    List<ChitPlan> findActivePlansWithCycles();

}
