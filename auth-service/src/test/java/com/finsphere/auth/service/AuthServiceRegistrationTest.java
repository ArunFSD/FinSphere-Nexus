package com.finsphere.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.finsphere.auth.config.AuthProperties;
import com.finsphere.auth.config.AuthProperties.ConcurrentSessionPolicy;
import com.finsphere.auth.domain.LoginAuditAction;
import com.finsphere.auth.domain.UserAccount;
import com.finsphere.auth.domain.UserRole;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.outbox.OutboxService;
import com.finsphere.auth.repository.UserAccountRepository;
import com.finsphere.auth.session.LoginAttemptService;
import com.finsphere.auth.session.SessionRegistry;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserProfileResponse;
import com.finsphere.common.event.KafkaTopics;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.JwtService;
import com.finsphere.common.web.ClientInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    private static final ClientInfo CLIENT = new ClientInfo("10.0.0.1", "JUnit");

    @Mock
    private UserAccountRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private SessionRegistry sessions;

    @Mock
    private LoginAttemptService loginAttempts;

    @Mock
    private LoginAuditService loginAudit;

    @Mock
    private OutboxService outbox;

    private AuthService service;

    @BeforeEach
    void setUp() {
        UserMapper mapper = Mappers.getMapper(UserMapper.class);
        AuthProperties properties = new AuthProperties(ConcurrentSessionPolicy.REPLACE,
                new AuthProperties.Login(5, Duration.ofMinutes(15)));
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T05:00:00Z"), ZoneOffset.UTC);
        this.service = new AuthService(this.users, this.passwordEncoder, mapper, this.jwtService, this.sessions,
                this.loginAttempts, this.loginAudit, this.outbox, properties, clock);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customersRegisterWithNormalisedIdentifiersAndAnEventIsQueued() {
        when(this.passwordEncoder.encode("Secret123")).thenReturn("{bcrypt}hash");
        when(this.users.saveAndFlush(any(UserAccount.class))).thenAnswer((call) -> {
            UserAccount account = call.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 42L);
            return account;
        });

        UserProfileResponse profile = this.service.register(request(" Ravi@Example.com ", "CUSTOMER"), CLIENT);

        assertThat(profile.userId()).isEqualTo(42L);
        assertThat(profile.phoneNumber()).isEqualTo("9876543210");
        assertThat(profile.email()).isEqualTo("ravi@example.com");
        assertThat(profile.fullName()).isEqualTo("Ravi Kumar");
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(this.outbox).enqueue(eq(KafkaTopics.USER_UPDATES), eq("42"), event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(UserUpdateEvent.class, (update) -> {
            assertThat(update.userId()).isEqualTo(42L);
            assertThat(update.fullName()).isEqualTo("Ravi Kumar");
            assertThat(update.role()).isEqualTo("CUSTOMER");
            assertThat(update.active()).isTrue();
        });
        verify(this.loginAudit).record(eq(LoginAuditAction.REGISTER_SUCCESS), eq(42L), eq("9876543210"),
                eq("ravi@example.com"), eq(CLIENT), anyString());
    }

    @Test
    void administratorsCannotSelfRegisterOnceOneExists() {
        when(this.users.existsByRole(UserRole.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> this.service.register(request(null, "ADMIN"), CLIENT))
            .isInstanceOfSatisfying(DomainException.class,
                    (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(this.users, never()).saveAndFlush(any());
        verify(this.loginAudit).record(eq(LoginAuditAction.REGISTER_FAILURE), isNull(), eq("9876543210"), isNull(),
                eq(CLIENT), anyString());
    }

    @Test
    void anAdministratorMayCreateAnotherAdministrator() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "9000000001", "ADMIN", "Admin", null);
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(admin, null,
                    List.of(new SimpleGrantedAuthority(admin.authority()))));
        when(this.passwordEncoder.encode("Secret123")).thenReturn("{bcrypt}hash");
        when(this.users.saveAndFlush(any(UserAccount.class))).thenAnswer((call) -> call.getArgument(0));

        assertThat(this.service.register(request(null, "ADMIN"), CLIENT).role()).isEqualTo("ADMIN");
    }

    @Test
    void duplicatePhoneOrEmailIsAConflictListingTheFields() {
        when(this.users.existsByPhoneNumber("9876543210")).thenReturn(true);
        when(this.users.existsByEmailIgnoreCase("ravi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> this.service.register(request("ravi@example.com", "CUSTOMER"), CLIENT))
            .isInstanceOfSatisfying(DomainException.class, (ex) -> {
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(ex.getErrors()).containsOnlyKeys("phoneNumber", "email");
            });
    }

    @Test
    void aLostRaceOnTheUniqueIndexIsStillAConflict() {
        when(this.passwordEncoder.encode("Secret123")).thenReturn("{bcrypt}hash");
        when(this.users.saveAndFlush(any(UserAccount.class)))
            .thenThrow(new DataIntegrityViolationException("uk_users_phone_number"));

        assertThatThrownBy(() -> this.service.register(request(null, "CUSTOMER"), CLIENT))
            .isInstanceOfSatisfying(DomainException.class,
                    (ex) -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(this.outbox, never()).enqueue(anyString(), anyString(), any());
    }

    private static RegistrationRequest request(String email, String role) {
        return new RegistrationRequest("Ravi Kumar", "+91 9876543210", email, "Secret123", "12 Anna Salai, Chennai",
                "S/o Mani", role);
    }
}
