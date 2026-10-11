package com.finsphere.chit.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.domain.PaymentTransaction;
import com.finsphere.chit.domain.UserMirror;
import com.finsphere.chit.mapper.TransactionMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.chit.repository.jpa.LoanRepository;
import com.finsphere.chit.repository.mongo.PaymentTransactionRepository;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.audit.Auditable;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.web.CorrelationId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * The payment log book (MongoDB). A payment must belong to an active member and reference a scheme the member is
 * enrolled in, or a loan of that member.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final String PAYMENT_TARGET_MISMATCH = "This payment does not match the member's schemes or loans.";

    private final PaymentTransactionRepository transactions;

    private final ChitEnrollmentRepository enrollments;

    private final LoanRepository loans;

    private final TransactionMapper mapper;

    private final UserDirectoryService directory;

    public TransactionService(PaymentTransactionRepository transactions, ChitEnrollmentRepository enrollments,
            LoanRepository loans, TransactionMapper mapper, UserDirectoryService directory) {
        this.transactions = transactions;
        this.enrollments = enrollments;
        this.loans = loans;
        this.mapper = mapper;
        this.directory = directory;
    }

    @Auditable(action = "PAYMENT_RECORDED", resource = "PAYMENT", resourceId = "#result.id")
    public TransactionResponse record(TransactionRequest request) {
        UserMirror payer = this.directory.requireActiveMember(request.userId());
        validateTarget(request);
        PaymentTransaction transaction = this.mapper.toDocument(request);
        transaction.setCorrelationId(CorrelationId.current());
        PaymentTransaction saved = this.transactions.save(transaction);
        log.info("Recorded {} of {} for user {} against {}", saved.getTransactionType(), saved.getAmount(),
                saved.getUserId(), saved.getTargetId());
        return this.mapper.toResponse(saved, payer.getFullName());
    }

    public PageResponse<TransactionResponse> historyOf(Long userId, Pageable pageable) {
        return withNames(this.transactions.findByUserId(userId, pageable));
    }

    public PageResponse<TransactionResponse> recent(Pageable pageable) {
        return withNames(this.transactions.findAll(pageable));
    }

    private PageResponse<TransactionResponse> withNames(Page<PaymentTransaction> page) {
        Map<Long, String> names = this.directory
            .namesOf(page.getContent().stream().map(PaymentTransaction::getUserId).collect(Collectors.toSet()));
        return PageResponse.from(page, (payment) -> this.mapper.toResponse(payment, names.get(payment.getUserId())));
    }

    private void validateTarget(TransactionRequest request) {
        switch (request.transactionType()) {
            case CHIT_PAYMENT -> {
                if (this.enrollments.countByPlanIdAndUserIdAndActiveTrue(request.targetId(), request.userId()) == 0) {
                    throw DomainException.badRequest(PAYMENT_TARGET_MISMATCH, "targetId",
                            "The member is not enrolled in scheme " + request.targetId());
                }
            }
            case LOAN_PAYMENT -> {
                if (this.loans.findByIdAndUserId(request.targetId(), request.userId()).isEmpty()) {
                    throw DomainException.badRequest(PAYMENT_TARGET_MISMATCH, "targetId",
                            "Loan " + request.targetId() + " does not belong to this member");
                }
            }
        }
    }
}
