package com.finsphere.chit.repository.jpa;

import java.util.List;
import java.util.Optional;

import com.finsphere.chit.domain.Loan;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderByStartDateDesc(Long userId);

    Optional<Loan> findByIdAndUserId(Long id, Long userId);
}
