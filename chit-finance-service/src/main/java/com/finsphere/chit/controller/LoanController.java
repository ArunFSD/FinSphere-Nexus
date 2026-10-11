package com.finsphere.chit.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.LoanService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.common.validation.ValidationGroups;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.LOANS)
@Tag(name = "Loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @Operation(summary = "Disburse a daily-collection loan (admin)", description = """
            The member receives `principalAmount` minus `interestDeduction` and repays `principalAmount` in equal \
            daily installments over `tenureDays` collection days from `startDate`. `leaveType` sets the days \
            without collection: NONE, SUNDAY or WEEKEND. 404 when the member is unknown, 400 when the interest is \
            not less than the loan amount or a value is out of range.""")
    @PostMapping("/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(
            @Validated(ValidationGroups.Sequence.class) @RequestBody LoanRequest request) {
        LoanResponse loan = this.loanService.disburse(request);
        return ApiResponse.created("Loan issued. Collection ends on " + loan.endDate(), loan).toResponseEntity();
    }

    @Operation(summary = "All loans, newest first (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponse>>> all(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Loans", this.loanService.allLoans(pageable)).toResponseEntity();
    }

    @Operation(summary = "Loans of the signed-in user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> mine() {
        Long userId = SecurityUtils.requireCurrentUser().userId();
        return ApiResponse.ok("Your loans", this.loanService.loansOf(userId)).toResponseEntity();
    }

    @Operation(summary = "Loans of a member (admin, or the member)")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> ofUser(@PathVariable Long userId) {
        return ApiResponse.ok("Loans", this.loanService.loansOf(userId)).toResponseEntity();
    }
}
