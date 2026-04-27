package com.finsphere.repository.jpa;

import com.finsphere.entity.finance.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdAndIsActiveTrue(Long userId);

}
