package com.finsphere.chit.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.calc.LoanScheduleCalculator;
import com.finsphere.chit.calc.LoanScheduleCalculator.LoanTerms;
import com.finsphere.chit.config.ChitProperties;
import com.finsphere.chit.domain.Loan;
import com.finsphere.chit.domain.UserMirror;
import com.finsphere.chit.mapper.LoanMapper;
import com.finsphere.chit.repository.jpa.LoanRepository;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.audit.Auditable;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.exception.DomainException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily-collection loans.
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loans;

    private final LoanMapper mapper;

    private final UserDirectoryService directory;

    private final ChitProperties properties;

    private final Clock clock;

    public LoanService(LoanRepository loans, LoanMapper mapper, UserDirectoryService directory,
            ChitProperties properties, Clock clock) {
        this.loans = loans;
        this.mapper = mapper;
        this.directory = directory;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    @Auditable(action = "LOAN_DISBURSED", resource = "LOAN", resourceId = "#result.id")
    public LoanResponse disburse(LoanRequest request) {
        UserMirror borrower = this.directory.requireActiveMember(request.userId());
        if (request.interestDeduction().compareTo(request.principalAmount()) >= 0) {
            throw DomainException.badRequest("Some loan details need changing.", "interestDeduction",
                    "Interest must be less than the loan amount");
        }
        LoanTerms terms = LoanScheduleCalculator.terms(request.principalAmount(), request.interestDeduction(),
                request.tenureDays(), request.startDate(), request.leaveType());
        Loan loan = this.mapper.toEntity(request);
        loan.setDisbursedAmount(terms.disbursedAmount());
        loan.setDailyInstallment(terms.dailyInstallment());
        loan.setEndDate(terms.endDate());
        loan.setAmountGivenDate(today());
        Loan saved = this.loans.save(loan);
        log.info("Loan {} disbursed to user {}: {} over {} collection days, ends {}", saved.getId(),
                saved.getUserId(), saved.getPrincipalAmount(), saved.getTenureDays(), saved.getEndDate());
        return this.mapper.toResponse(saved, borrower.getFullName());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> loansOf(Long userId) {
        return withNames(this.loans.findByUserIdOrderByStartDateDesc(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> allLoans(Pageable pageable) {
        Page<Loan> page = this.loans.findAll(pageable);
        Map<Long, String> names = this.directory
            .namesOf(page.getContent().stream().map(Loan::getUserId).collect(Collectors.toSet()));
        return PageResponse.from(page, (loan) -> this.mapper.toResponse(loan, names.get(loan.getUserId())));
    }

    private List<LoanResponse> withNames(List<Loan> list) {
        Map<Long, String> names = this.directory
            .namesOf(list.stream().map(Loan::getUserId).collect(Collectors.toSet()));
        return list.stream().map((loan) -> this.mapper.toResponse(loan, names.get(loan.getUserId()))).toList();
    }

    private LocalDate today() {
        return LocalDate.now(this.clock.withZone(this.properties.zone()));
    }
}
