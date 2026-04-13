package com.finsphere.auth.service;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.CustomerProfile;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.UserRole;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.model.UserContext;
import com.finsphere.auth.model.UserSession;
import com.finsphere.auth.repository.UserRepository;
import com.finsphere.auth.security.JwtUtils;
import com.finsphere.auth.util.CookieUtils;
import com.finsphere.auth.util.RedisUtils;
import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.dto.events.UserUpdateEvent;
import com.finsphere.common.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtils jwt;
    private final CookieUtils cookie;
    private final RedisUtils redis;
    private final AuditService auditService;
    private final UserEventProducer userEventProducer;

    @Transactional
    public ApiResponse<Void> registerUser(
            RegistrationRequest request,
            String ipAddress,
            String userAgent
    ) throws Exception {

        log.info(">>>> [AUTH_REG_START] Registration attempt for Phone: {} | Email: {}",
                request.getPhoneNumber(), request.getEmail());

        Map<String, String> businessErrors = new HashMap<>();

        // 1. Business Validation
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            businessErrors.put("phoneNumber", "Phone number is already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                businessErrors.put("email", "Email is already registered");
            }
        }

        if (!businessErrors.isEmpty()) {

            log.warn("!!!! [AUTH_REG_VALIDATION_FAIL] Validation failed for: {} | Errors: {}",
                    request.getPhoneNumber(), businessErrors.keySet());

            auditService.record(
                    null,
                    request.getPhoneNumber(),
                    request.getEmail(),
                    "REGISTER_FAILURE",
                    ipAddress, userAgent,
                    "Validation failed: " + businessErrors.values()
            );

            throw new DomainException(HttpStatus.BAD_REQUEST, "Validation Failed", businessErrors);
        }

        // 2. Normalization & Mapping
        String rawPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");
        String normalizedPhone = rawPhone.substring(Math.max(0, rawPhone.length() - 10));
        request.setPhoneNumber(normalizedPhone);

        User user = userMapper.toEntity(request);
        CustomerProfile profile = userMapper.toProfile(request);
        UserRole role = (request.getRole() != null)
                ? UserRole.valueOf(request.getRole().toUpperCase())
                : UserRole.CUSTOMER;

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        user.setProfile(profile);
        profile.setUser(user);

        // 3. Persistence
        User savedUser = userRepository.save(user);
        log.info("<<<< [AUTH_REG_DB_SUCCESS] User persisted with ID: {}", savedUser.getId());

        // 4. Audit Success
        auditService.record(
                savedUser.getId(),
                savedUser.getPhoneNumber(),
                savedUser.getEmail(),
                "REGISTER_SUCCESS",
                ipAddress, userAgent,
                "New account created"
        );

        // 5. Kafka Event
        log.info(">>>> [AUTH_KAFKA_DISPATCH] Sending UserUpdateEvent for ID: {}", savedUser.getId());

        UserUpdateEvent event = new UserUpdateEvent(
                savedUser.getId(),
                profile.getFullName(),
                savedUser.getPhoneNumber(),
                profile.getCareOf(),
                savedUser.getRole().name(),
                savedUser.getIsActive()
        );
        userEventProducer.sendUserUpdate(event);

        return ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.CREATED.value())
                .message("Registration successful for " + profile.getFullName())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public ApiResponse<Map<String, String>> login(
            LoginRequest loginRequest,
            String ipAddress,
            String userAgent,
            HttpServletResponse response
    ) throws Exception {

        log.info(">>>> [AUTH_LOGIN_START] Login attempt for Identifier: {}", loginRequest.getIdentifier());

        String identifier = loginRequest.getIdentifier();
        boolean isEmailInput = identifier.contains("@");

        try {
            User user = userRepository.findByIdentifier(identifier)
                    .orElseThrow(() -> new DomainException(
                            HttpStatus.UNAUTHORIZED,
                            "Authentication Failed",
                            "login",
                            "Invalid credentials")
                    );

            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("!!!! [AUTH_LOGIN_FAIL] Incorrect password for: {}", identifier);
                throw new DomainException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "login",
                        "Your password is incorrect"
                );
            }

            if (!redis.getSessionDetails(user).isEmpty()) {
                log.warn("!!!! [AUTH_LOGIN_CONFLICT] Active session exists for: {}", identifier);
                throw new DomainException(
                        HttpStatus.CONFLICT,
                        "Active Session Found",
                        "login",
                        "Already logged in elsewhere"
                );
            }

            String token = jwt.generateToken(user.getPhoneNumber(), user.getRole().name());
            redis.saveSessionToRedis(token, user, ipAddress, userAgent);
            cookie.setHttpOnlyCookie(response, token);

            log.info("<<<< [AUTH_LOGIN_SUCCESS] User logged in: {} | Role: {}", user.getPhoneNumber(), user.getRole());
            auditService.record(
                    user.getId(),
                    user.getPhoneNumber(),
                    user.getEmail(),
                    "LOGIN_SUCCESS",
                    ipAddress, userAgent,
                    "Authentication successful"
            );

            return ApiResponse.<Map<String, String>>builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Login successful")
                    .data(Map.of("role", user.getRole().name()))
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("!!!! [AUTH_LOGIN_ERROR] Login failure: {}", e.getMessage());
            auditService.record(
                    null,
                    isEmailInput ? null : identifier,
                    isEmailInput ? identifier : null,
                    "LOGIN_FAILURE",
                    ipAddress, userAgent,
                    e.getMessage()
            );
            throw e;
        }
    }

    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        String token = cookie.extractToken(request);
        log.info(">>>> [AUTH_LOGOUT_START] Processing logout");

        if (token != null && !token.isBlank()) {
            redis.getSessionDetails(token).ifPresent(session -> {

                log.info("<<<< [AUTH_LOGOUT_REDIS] Clearing session for UserID: {}", session.getUserId());

                auditService.record(
                        session.getUserId(),
                        session.getPhoneNumber(),
                        null,
                        "LOGOUT",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"),
                        "User logged out successfully"
                );

                redis.delSessionToRedis(token);
            });
        }

        cookie.delHttpOnlyCookie(response);
        SecurityContextHolder.clearContext();

        return ApiResponse.<Void>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public UserContext validateSession(String token) {
        log.debug(">>>> [AUTH_VALIDATE_START] Validating session token");
        if (token == null || token.isEmpty()) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Security Alert", "token", "No session found");
        }
        if (!jwt.validateToken(token)) {
            log.warn("!!!! [AUTH_VALIDATE_FAIL] JWT validation failed or expired");
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Session Expired", "token", "Please login again");
        }

        UserSession session = redis.getSessionDetails(token)
                .orElseThrow(() -> {
                    log.warn("!!!! [AUTH_VALIDATE_FAIL] Redis session missing for valid JWT");
                    return new DomainException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid Session",
                            "token",
                            "Session terminated"
                    );
                });

        return UserContext.builder()
                .userId(session.getUserId())
                .phoneNumber(session.getPhoneNumber())
                .role(session.getRole())
                .build();
    }
}