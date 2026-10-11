package com.finsphere.chit.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.finsphere.chit.domain.ChitMonthlyCycle;
import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.chit.mapper.ChitCycleMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.chit.repository.jpa.ChitMonthlyCycleRepository;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.AuctionResultResponse;
import com.finsphere.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    private static final long PLAN_ID = 1L;

    private static final long WINNER = 7L;

    @Mock
    private ChitMonthlyCycleRepository cycles;

    @Mock
    private ChitEnrollmentRepository enrollments;

    private final ChitCycleMapper mapper = Mappers.getMapper(ChitCycleMapper.class);

    private AuctionService service;

    private ChitMonthlyCycle cycle;

    @BeforeEach
    void setUp() {
        this.service = new AuctionService(this.cycles, this.enrollments, this.mapper);
        ChitPlan plan = new ChitPlan();
        ReflectionTestUtils.setField(plan, "id", PLAN_ID);
        plan.setName("Gold 1L");
        plan.setTotalValue(new BigDecimal("100000"));
        plan.setCommissionPercentage(new BigDecimal("5"));
        plan.setMonthlyInstallment(new BigDecimal("5000"));
        plan.setMaxBidLimit(new BigDecimal("40000"));
        plan.setTotalMembers(20);
        plan.setDurationMonths(20);
        plan.setStartDate(LocalDate.of(2026, 1, 1));
        this.cycle = new ChitMonthlyCycle(plan, 3, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10),
                plan.getMonthlyInstallment());
    }

    @Test
    void settlesTheMonthAndSharesTheDividend() {
        when(this.cycles.findByPlanIdAndMonthlyCount(PLAN_ID, 3)).thenReturn(Optional.of(this.cycle));
        when(this.enrollments.countByPlanIdAndUserIdAndActiveTrue(PLAN_ID, WINNER)).thenReturn(2L);
        when(this.cycles.countByPlanIdAndWinnerUserId(PLAN_ID, WINNER)).thenReturn(1L);
        when(this.cycles.saveAndFlush(any(ChitMonthlyCycle.class))).thenAnswer((call) -> call.getArgument(0));

        AuctionResultResponse result = this.service.settle(request("12000"));

        assertThat(result.prizeMoney()).isEqualByComparingTo("88000");
        assertThat(result.organiserCommission()).isEqualByComparingTo("5000");
        assertThat(result.winNumber()).isEqualTo(2);
        assertThat(result.slotsHeld()).isEqualTo(2);
        assertThat(result.cycle().closed()).isTrue();
        assertThat(result.cycle().winnerUserId()).isEqualTo(WINNER);
        assertThat(result.cycle().dividendPerMember()).isEqualByComparingTo("350");
        assertThat(result.cycle().monthlyPayableAmount()).isEqualByComparingTo("4650");
    }

    @Test
    void aSettledMonthCannotBeSettledAgain() {
        this.cycle.settle(WINNER, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("5000"));
        when(this.cycles.findByPlanIdAndMonthlyCount(PLAN_ID, 3)).thenReturn(Optional.of(this.cycle));

        assertThatThrownBy(() -> this.service.settle(request("12000"))).isInstanceOfSatisfying(DomainException.class,
                (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(this.cycles, never()).saveAndFlush(any());
    }

    @Test
    void bidsAboveTheSchemeLimitAreRejected() {
        when(this.cycles.findByPlanIdAndMonthlyCount(PLAN_ID, 3)).thenReturn(Optional.of(this.cycle));

        assertThatThrownBy(() -> this.service.settle(request("40000.01")))
            .isInstanceOfSatisfying(DomainException.class, (ex) -> {
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(ex.getErrors()).containsKey("bidAmount");
            });
    }

    @Test
    void theWinnerMustHoldAnUnusedSlot() {
        when(this.cycles.findByPlanIdAndMonthlyCount(PLAN_ID, 3)).thenReturn(Optional.of(this.cycle));
        when(this.enrollments.countByPlanIdAndUserIdAndActiveTrue(PLAN_ID, WINNER)).thenReturn(0L, 1L);
        when(this.cycles.countByPlanIdAndWinnerUserId(PLAN_ID, WINNER)).thenReturn(1L);

        assertThatThrownBy(() -> this.service.settle(request("12000"))).isInstanceOfSatisfying(DomainException.class,
                (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> this.service.settle(request("12000"))).isInstanceOfSatisfying(DomainException.class,
                (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unknownMonthIsNotFound() {
        when(this.cycles.findByPlanIdAndMonthlyCount(PLAN_ID, 3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.settle(request("12000"))).isInstanceOfSatisfying(DomainException.class,
                (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static AuctionRequest request(String bid) {
        return new AuctionRequest(PLAN_ID, 3, new BigDecimal(bid), WINNER);
    }
}
