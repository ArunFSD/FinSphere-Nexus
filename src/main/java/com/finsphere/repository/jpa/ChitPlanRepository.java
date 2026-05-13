package com.finsphere.repository.jpa;

import com.finsphere.entity.chit.ChitPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChitPlanRepository extends JpaRepository<ChitPlan, Long> {

    Page<ChitPlan> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT DISTINCT p FROM ChitPlan p LEFT JOIN FETCH p.monthlyCycles WHERE p.isActive = true")
    List<ChitPlan> findActivePlansWithCycles();

}
