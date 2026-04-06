package com.finsphere.repository;

import com.finsphere.entity.ChitMonthlyCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChitMonthlyCycleRepository extends JpaRepository<ChitMonthlyCycle, Long> {

}
