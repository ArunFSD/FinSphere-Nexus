package com.finsphere.auth.service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.auth.config.AuthProperties;
import com.finsphere.auth.config.AuthProperties.ConcurrentSessionPolicy;
import com.finsphere.auth.domain.LoginAuditAction;
import com.finsphere.auth.domain.UserAccount;
import com.finsphere.auth.domain.UserRole;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.outbox.OutboxService;
import com.finsphere.auth.repository.UserAccountRepository;
import com.finsphere.auth.session.LoginAttemptService;
import com.finsphere.auth.session.SessionInfo;
import com.finsphere.auth.session.SessionRegistry;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserContext;
import com.finsphere.common.dto.auth.UserProfileResponse;
import com.finsphere.common.event.KafkaTopics;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.exception.DomainException;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.JwtService;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.common.web.ClientInfo;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, logout and session validation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String INVALID_CREDENTIALS = "The mobile number, e-mail or password is not correct.";

    private static final String CHECK_FIELDS = "Some details need changing. Check the highlighted fields.";

    private final UserAccountRepository users;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final JwtService jwtService;

    private final SessionRegistry sessions;

    private final LoginAttemptService loginAttempts;

    private final LoginAuditService loginAudit;

    private final OutboxService outbox;

    private final AuthProperties properties;

    private final Clock clock;

    /** Compared against when the identifier is unknown, so response time does not reveal which accounts exist. */
    private final String timingSafeDummyHash;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, UserMapper userMapper,
            JwtService jwtService, SessionRegistry sessions, LoginAttemptService loginAttempts,
            LoginAuditService loginAudit, OutboxService outbox, AuthProperties properties, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.loginAttempts = loginAttempts;
        this.loginAudit = loginAudit;
        this.outbox = outbox;
        this.properties = properties;
        this.clock = clock;
        this.timingSafeDummyHash = passwordEncoder.encode("timing-safe-dummy-password");
    }

    /**
     * Creates an account and its profile, and queues a user update event for other services (same transaction).
     * {@code ADMIN} accounts need an administrator caller, except for the very first administrator.
     */
    @Transactional
    public UserProfileResponse register(RegistrationRequest request, ClientInfo client) {
        String phone = IdentifierNormalizer.phone(request.phoneNumber());
        String email = IdentifierNormalizer.email(request.email());
        UserRole role = UserRole.valueOf(request.role());

        if (role == UserRole.ADMIN && !SecurityUtils.isAdmin() && this.users.existsByRole(UserRole.ADMIN)) {
            this.loginAudit.record(LoginAuditAction.REGISTER_FAILURE, null, phone, email, client,
                    "Administrator self-registration refused");
            throw new DomainException(HttpStatus.FORBIDDEN, "Only an administrator can add another administrator.",
                    "role", "Sign up as a member, or ask an administrator to add you");
        }

        Map<String, String> conflicts = new LinkedHashMap<>();
        if (this.users.existsByPhoneNumber(phone)) {
            conflicts.put("phoneNumber", "This mobile number already has an account");
        }
        if (email != null && this.users.existsByEmailIgnoreCase(email)) {
            conflicts.put("email", "This e-mail address already has an account");
        }
        if (!conflicts.isEmpty()) {
            this.loginAudit.record(LoginAuditAction.REGISTER_FAILURE, null, phone, email, client,
                    "Already registered: " + conflicts.keySet());
            throw new DomainException(HttpStatus.CONFLICT, CHECK_FIELDS, conflicts);
        }

        UserAccount account = new UserAccount(phone, email, this.passwordEncoder.encode(request.password()), role);
        account.attachProfile(this.userMapper.toProfile(request));
        UserAccount saved;
        try {
            saved = this.users.saveAndFlush(account);
        }
        catch (DataIntegrityViolationException ex) {
            // Two registrations raced for the same phone or e-mail; the unique indexes kept the data consistent
            throw new DomainException(HttpStatus.CONFLICT, CHECK_FIELDS, "phoneNumber",
                    "This mobile number or e-mail address already has an account");
        }

        UserUpdateEvent event = this.userMapper.toUpdateEvent(saved, this.clock.instant());
        this.outbox.enqueue(KafkaTopics.USER_UPDATES, String.valueOf(saved.getId()), event);
        this.loginAudit.record(LoginAuditAction.REGISTER_SUCCESS, saved.getId(), phone, email, client,
                "Account created with role " + role);
        log.info("Registered user {} with role {}", saved.getId(), role);
        return this.userMapper.toProfileResponse(saved);
    }

    public LoginResult login(LoginRequest request, ClientInfo client) {
        String identifier = IdentifierNormalizer.identifier(request.identifier());
        try {
            this.loginAttempts.assertNotLocked(identifier);
        }
        catch (DomainException locked) {
            recordLoginEvent(LoginAuditAction.LOGIN_BLOCKED, null, identifier, client, "Identifier locked");
            throw locked;
        }

        Optional<UserAccount> found = this.users.findByIdentifier(identifier);
        if (found.isEmpty()) {
            this.passwordEncoder.matches(request.password(), this.timingSafeDummyHash);
            throw failedLogin(identifier, null, client, "Unknown identifier");
        }
        UserAccount user = found.get();
        if (!this.passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw failedLogin(identifier, user.getId(), client, "Wrong password");
        }
        if (!user.isActive()) {
            recordLoginEvent(LoginAuditAction.LOGIN_FAILURE, user.getId(), identifier, client, "Account disabled");
            throw new DomainException(HttpStatus.FORBIDDEN, "This account is disabled.", "identifier",
                    "Ask your branch office to turn it back on");
        }

        applySessionPolicy(user, identifier, client);
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getPhoneNumber(), user.getRole().name(),
                user.displayName(), user.getEmail());
        String token = this.jwtService.issue(principal);
        this.sessions.register(token, principal, client);
        this.loginAttempts.reset(identifier);
        recordLoginEvent(LoginAuditAction.LOGIN_SUCCESS, user.getId(), identifier, client, "Signed in");
        return new LoginResult(token, this.userMapper.toLoginResponse(user));
    }

    /** Revokes the session behind the token, if any. Always succeeds so the cookie can be cleared. */
    public void logout(Optional<String> token, ClientInfo client) {
        token.ifPresent((raw) -> this.jwtService.parse(raw).ifPresent((user) -> {
            boolean revoked = this.sessions.revoke(user.userId(), raw);
            recordLoginEvent(LoginAuditAction.LOGOUT, user.userId(), user.identifier(), client,
                    revoked ? "Signed out" : "Session had already ended");
        }));
    }

    /** Checks signature, expiry and that the session was not revoked. */
    public UserContext validateSession(Optional<String> token) {
        SessionInfo session = requireLiveSession(token);
        return new UserContext(session.userId(), session.phoneNumber(), session.role());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse currentProfile(Optional<String> token) {
        SessionInfo session = requireLiveSession(token);
        UserAccount user = this.users.findWithProfileById(session.userId())
            .orElseThrow(() -> DomainException.notFound("Account not found.", "userId",
                    "The account no longer exists"));
        return this.userMapper.toProfileResponse(user);
    }

    private SessionInfo requireLiveSession(Optional<String> token) {
        String raw = token.orElseThrow(
                () -> DomainException.unauthorized("Authentication required", "session", "No session found"));
        AuthenticatedUser user = this.jwtService.parse(raw)
            .orElseThrow(() -> DomainException.unauthorized("Your session has expired. Sign in again.", "session",
                    "Sign in again"));
        return this.sessions.find(user.userId(), raw)
            .orElseThrow(() -> DomainException.unauthorized("Your session has ended. Sign in again.", "session",
                    "Sign in again"));
    }

    private void applySessionPolicy(UserAccount user, String identifier, ClientInfo client) {
        if (this.properties.concurrentSessionPolicy() == ConcurrentSessionPolicy.REJECT) {
            if (this.sessions.activeSessions(user.getId()) > 0) {
                recordLoginEvent(LoginAuditAction.LOGIN_FAILURE, user.getId(), identifier, client,
                        "Rejected: active session elsewhere");
                throw new DomainException(HttpStatus.CONFLICT, "You are already signed in on another device.", "login",
                        "Sign out from the other device first");
            }
            return;
        }
        long replaced = this.sessions.revokeAll(user.getId());
        if (replaced > 0) {
            recordLoginEvent(LoginAuditAction.SESSION_REPLACED, user.getId(), identifier, client,
                    replaced + " older session(s) signed out");
        }
    }

    private DomainException failedLogin(String identifier, @Nullable Long userId, ClientInfo client, String reason) {
        long attemptsLeft = this.loginAttempts.recordFailure(identifier);
        recordLoginEvent(LoginAuditAction.LOGIN_FAILURE, userId, identifier, client, reason);
        String hint = (attemptsLeft > 0)
                ? "Check your credentials. " + attemptsLeft + " attempt(s) left before a temporary lock."
                : "Too many failed attempts. Sign-in is locked for "
                        + this.loginAttempts.lockoutDuration().toMinutes() + " minute(s).";
        return new DomainException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS, "password", hint);
    }

    private void recordLoginEvent(LoginAuditAction action, @Nullable Long userId, String identifier,
            ClientInfo client, String remarks) {
        boolean email = IdentifierNormalizer.isEmail(identifier);
        this.loginAudit.record(action, userId, email ? null : identifier, email ? identifier : null, client, remarks);
    }
}
