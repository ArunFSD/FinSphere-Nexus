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
    private final SecurityUtils securityUtils; // From Common Library

    @Transactional
    public ApiResponse<Transaction> recordPayment(TransactionRequest request) {

        Transaction tx = Transaction.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .targetId(request.getTargetId())
                .paymentMode(request.getPaymentMode())
                .remarks(request.getRemarks())
                .createdBy(securityUtils.getCurrentUserId())
                .receivedByInfo(request.getReceivedByInfo())
                .paymentDate(request.getPaymentDate())
                .build();

        return ApiResponse.<Transaction>builder()
                .success(true)
                .data(transactionRepository.save(tx))
                .message("Log Book updated in MongoDB")
                .build();
    }

    public ApiResponse<List<Transaction>> getUserTransactions(Long userId) {
        List<Transaction> history = transactionRepository.findByUserIdOrderByPaymentDateDesc(userId);
        return ApiResponse.<List<Transaction>>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .data(history)
                .build();
    }
}
