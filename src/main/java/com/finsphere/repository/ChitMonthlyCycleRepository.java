package com.finsphere.repository;

import com.finsphere.entity.ChitMonthlyCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChitMonthlyCycleRepository extends JpaRepository<ChitMonthlyCycle, Long> {

    Optional<ChitMonthlyCycle> findByPlanIdAndMonthlyCount(Long planId, Integer monthlyCount);

}
