package com.finsphere.service;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.dto.TransactionRequest;
import com.finsphere.entity.Transaction;
import com.finsphere.repository.mongo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public ApiResponse<Transaction> recordPayment(TransactionRequest request) {
        Long currentAdmin = securityUtils.getCurrentUserId();

        log.info(">>>> [TX_START] Recording {} for User: {} | Amount: {} | Target: {} | By: {}",
                request.getTransactionType(), request.getUserId(), request.getAmount(),
                request.getTargetId(), currentAdmin);

        Transaction tx = Transaction.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .targetId(request.getTargetId())
                .paymentMode(request.getPaymentMode())
                .remarks(request.getRemarks())
                .createdBy(currentAdmin)
                .receivedByInfo(request.getReceivedByInfo())
                .paymentDate(request.getPaymentDate())
                .build();

        Transaction savedTx = transactionRepository.save(tx);

        log.info("<<<< [TX_SUCCESS] Transaction logged in MongoDB with ID: {} for User: {}",
                savedTx.getId(), request.getUserId());

        return ApiResponse.<Transaction>builder()
                .success(true)
                .data(savedTx)
                .message("Log Book updated in MongoDB")
                .build();
    }

    public ApiResponse<List<Transaction>> getUserTransactions(Long userId) {
        log.info(">>>> [TX_FETCH] Fetching transaction history for User: {}", userId);

        List<Transaction> history = transactionRepository.findByUserIdOrderByPaymentDateDesc(userId);

        log.info("<<<< [TX_FETCH_SUCCESS] Found {} transactions for User: {}", history.size(), userId);

        return ApiResponse.<List<Transaction>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .data(history)
                .build();
    }
}