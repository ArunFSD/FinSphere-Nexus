package com.finsphere.service.finance;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.dto.LoanRequest;
import com.finsphere.entity.finance.Loan;
import com.finsphere.repository.jpa.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;

    @Transactional
    public ApiResponse<Loan> createLoan(LoanRequest request) {
        log.info(">>>> [LOAN_CREATE] User: {} | Principal: {} | Leaves: {}",
                request.getUserId(), request.getPrincipalAmount(), request.getLeaveType());

        BigDecimal disbursedAmount = request.getPrincipalAmount().subtract(request.getInterestDeduction());
        BigDecimal dailyInstallment = request.getPrincipalAmount()
                .divide(BigDecimal.valueOf(request.getTenureDays()), 2, RoundingMode.HALF_UP);

        // Calculate End Date by skipping leaves
        LocalDate endDate = calculateActualEndDate(request.getStartDate(), request.getTenureDays(), request.getLeaveType());

        Loan loan = Loan.builder()
                .userId(request.getUserId())
                .principalAmount(request.getPrincipalAmount())
                .disbursedAmount(disbursedAmount)
                .dailyInstallment(dailyInstallment)
                .tenureDays(request.getTenureDays())
                .amountGivenDate(LocalDate.now())
                .startDate(request.getStartDate())
                .endDate(endDate)
                .isActive(true)
                .build();

        return ApiResponse.<Loan>builder()
                .success(true)
                .data(loanRepository.save(loan))
                .message("Loan issued. Collection ends on: " + endDate)
                .build();
    }

    private LocalDate calculateActualEndDate(LocalDate start, int installments, String leaveType) {
        LocalDate current = start;
        int countedDays = 0;

        while (countedDays < installments) {
            boolean isLeave = false;
            java.time.DayOfWeek day = current.getDayOfWeek();

            if ("SUNDAY".equalsIgnoreCase(leaveType) && day == java.time.DayOfWeek.SUNDAY) {
                isLeave = true;
            } else if ("WEEKEND".equalsIgnoreCase(leaveType) &&
                    (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY)) {
                isLeave = true;
            }

            if (!isLeave) {
                countedDays++;
            }

            if (countedDays < installments) {
                current = current.plusDays(1);
            }
        }
        return current;
    }
}
