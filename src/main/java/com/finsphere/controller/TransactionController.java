package com.finsphere.controller;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.TransactionRequest;
import com.finsphere.entity.Transaction;
import com.finsphere.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.TRANSACTIONS)
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Transaction>> logPayment(
            @RequestBody TransactionRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [FINANCE_API_HIT] POST /finance/transactions/log | IP: {} | User: {} | Type: {} | Amount: {}",
                httpServletRequest.getRemoteAddr(), request.getUserId(),
                request.getTransactionType(), request.getAmount());

        ApiResponse<Transaction> response = transactionService.recordPayment(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getUserHistory(
            @PathVariable Long userId,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [FINANCE_API_HIT] GET /finance/transactions/history/{} | IP: {}",
                userId, httpServletRequest.getRemoteAddr());

        ApiResponse<List<Transaction>> response = transactionService.getUserTransactions(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}