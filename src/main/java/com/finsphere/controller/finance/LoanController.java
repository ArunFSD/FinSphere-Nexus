package com.finsphere.controller.finance;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.validation.ValidationGroups;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.LoanRequest;
import com.finsphere.entity.finance.Loan;
import com.finsphere.service.finance.LoanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.LOAN)
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Loan>> disburseLoan(
            @Validated(ValidationGroups.Sequence.class)
            @RequestBody LoanRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [FINANCE_API_HIT] POST /finance/loans/disburse | IP: {}", httpServletRequest.getRemoteAddr());
        ApiResponse<Loan> response = loanService.createLoan(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
