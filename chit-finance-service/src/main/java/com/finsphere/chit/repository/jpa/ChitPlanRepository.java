package com.finsphere.chit.repository.jpa;

import com.finsphere.chit.domain.ChitPlan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChitPlanRepository extends JpaRepository<ChitPlan, Long> {

    Page<ChitPlan> findByActiveTrue(Pageable pageable);
}
