package com.finsphere.chit.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.finsphere.chit.domain.ChitEnrollment;
import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.chit.domain.UserMirror;
import com.finsphere.chit.mapper.ChitEnrollmentMapper;
import com.finsphere.chit.repository.jpa.ChitEnrollmentRepository;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChitEnrollmentServiceTest {

    private static final long PLAN_ID = 1L;

    @Mock
    private ChitEnrollmentRepository enrollments;

    @Mock
    private ChitPlanService planService;

    @Mock
    private UserDirectoryService directory;

    private final ChitEnrollmentMapper mapper = Mappers.getMapper(ChitEnrollmentMapper.class);

    private ChitEnrollmentService service;

    private ChitPlan plan;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T05:00:00Z"), ZoneOffset.UTC);
        this.service = new ChitEnrollmentService(this.enrollments, this.planService, this.mapper, this.directory,
                clock);
        this.plan = new ChitPlan();
        ReflectionTestUtils.setField(this.plan, "id", PLAN_ID);
        this.plan.setName("Gold 1L");
        this.plan.setTotalMembers(20);
        this.plan.setTotalValue(new BigDecimal("100000"));
        this.plan.setStartDate(LocalDate.of(2026, 1, 1));
        this.plan.setActive(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customersCannotEnrolSomeoneElse() {
        signIn(10L, "CUSTOMER");

        assertThatThrownBy(() -> this.service.enroll(new EnrollmentRequest(PLAN_ID, 11L, 1)))
            .isInstanceOfSatisfying(DomainException.class,
                    (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void slotMustExistInThePlan() {
        signIn(10L, "CUSTOMER");
        when(this.planService.requirePlan(PLAN_ID)).thenReturn(this.plan);

        assertThatThrownBy(() -> this.service.enroll(new EnrollmentRequest(PLAN_ID, 10L, 21)))
            .isInstanceOfSatisfying(DomainException.class, (ex) -> {
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(ex.getErrors()).containsKey("slotNumber");
            });
    }

    @Test
    void aTakenSlotIsAConflictEvenWhenTheRaceIsLostInTheDatabase() {
        signIn(1L, "ADMIN");
        when(this.planService.requirePlan(PLAN_ID)).thenReturn(this.plan);
        when(this.directory.requireActiveMember(10L)).thenReturn(member(10L));
        when(this.enrollments.existsByPlanIdAndSlotNumberAndActiveTrue(PLAN_ID, 3)).thenReturn(true, false);
        when(this.enrollments.saveAndFlush(any(ChitEnrollment.class)))
            .thenThrow(new DataIntegrityViolationException("ux_enrollment_plan_slot_active"));

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> this.service.enroll(new EnrollmentRequest(PLAN_ID, 10L, 3)))
                .isInstanceOfSatisfying(DomainException.class,
                        (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    @Test
    void adminEnrolsAMemberIntoAFreeSlot() {
        signIn(1L, "ADMIN");
        when(this.planService.requirePlan(PLAN_ID)).thenReturn(this.plan);
        when(this.directory.requireActiveMember(10L)).thenReturn(member(10L));
        when(this.enrollments.existsByPlanIdAndSlotNumberAndActiveTrue(PLAN_ID, 3)).thenReturn(false);
        when(this.enrollments.saveAndFlush(any(ChitEnrollment.class))).thenAnswer((call) -> call.getArgument(0));

        ChitEnrollmentResponse response = this.service.enroll(new EnrollmentRequest(PLAN_ID, 10L, 3));

        assertThat(response.planId()).isEqualTo(PLAN_ID);
        assertThat(response.planName()).isEqualTo("Gold 1L");
        assertThat(response.memberName()).isEqualTo("Member 10");
        assertThat(response.slotNumber()).isEqualTo(3);
        assertThat(response.active()).isTrue();
        assertThat(response.joinedAt()).isEqualTo(Instant.parse("2026-10-09T05:00:00Z"));
    }

    private static UserMirror member(long userId) {
        UserMirror mirror = new UserMirror();
        mirror.setUserId(userId);
        mirror.setFullName("Member " + userId);
        mirror.setActive(true);
        return mirror;
    }

    private static void signIn(long userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "98765432" + (10 + userId), role, "Test user", null);
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null,
                    List.of(new SimpleGrantedAuthority(user.authority()))));
    }
}
