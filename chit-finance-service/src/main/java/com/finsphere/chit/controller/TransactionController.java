package com.finsphere.chit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.TransactionService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;
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
@RequestMapping(ApiPaths.TRANSACTIONS)
@Tag(name = "Payments")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Record a collected payment (admin)", description = """
            Adds an entry to the payment log book. `transactionType` CHIT_PAYMENT needs `targetId` to be a scheme \
            the member holds a slot in, LOAN_PAYMENT a loan of that member, otherwise 400 with `errors.targetId`. \
            `paymentMode` is CASH, UPI or BANK_TRANSFER, and `paymentDate` cannot be in the future.""")
    @PostMapping("/log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> log(
            @Validated(ValidationGroups.Sequence.class) @RequestBody TransactionRequest request) {
        TransactionResponse transaction = this.transactionService.record(request);
        return ApiResponse.created("Payment recorded in the log book", transaction).toResponseEntity();
    }

    @Operation(summary = "Latest payments across all members (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> recent(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Payments", this.transactionService.recent(pageable)).toResponseEntity();
    }

    @Operation(summary = "Payment history of the signed-in user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> mine(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = SecurityUtils.requireCurrentUser().userId();
        return ApiResponse.ok("Your payments", this.transactionService.historyOf(userId, pageable))
            .toResponseEntity();
    }

    @Operation(summary = "Payment history of a member (admin, or the member)")
    @GetMapping("/history/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> history(@PathVariable Long userId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok("Payment history", this.transactionService.historyOf(userId, pageable))
            .toResponseEntity();
    }
}
