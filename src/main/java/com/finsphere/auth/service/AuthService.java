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

        Map<String, String> businessErrors = new HashMap<>();

        // --- 1. VALIDATION & PROBING AUDIT ---
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            businessErrors.put("phoneNumber", "Phone number is already registered");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                businessErrors.put("email", "Email is already registered");
            }
        }

        if (!businessErrors.isEmpty()) {
            // Record the failure: Someone is trying to register with existing data
            auditService.record(
                    null,
                    request.getPhoneNumber(),
                    request.getEmail(),
                    "REGISTER_FAILURE",
                    ipAddress,
                    userAgent,
                    "Validation failed: " + businessErrors.values()
            );

            throw new DomainException(HttpStatus.BAD_REQUEST, "Validation Failed", businessErrors);
        }

        // --- 2. NORMALIZATION & MAPPING ---
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

        // --- 3. PERSISTENCE ---
        User savedUser = userRepository.save(user);

        // --- 4. SUCCESS AUDIT ---
        auditService.record(
                savedUser.getId(),
                savedUser.getPhoneNumber(),
                savedUser.getEmail(),
                "REGISTER_SUCCESS",
                ipAddress,
                userAgent,
                "New account created"
        );

        // --- 5. KAFKA PRODUCER
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

        String identifier = loginRequest.getIdentifier();
        boolean isEmailInput = identifier.contains("@");

        try {
            // 1. Find user by Phone or Email
            User user = userRepository.findByIdentifier(identifier)
                    .orElseThrow(() -> new DomainException(
                            HttpStatus.UNAUTHORIZED,
                            "Authentication Failed",
                            "login",
                            "Invalid credentials"
                    ));

            // 2. Validate Password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new DomainException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "login", "Your password is incorrect"
                );
            }

            if (!redis.getSessionDetails(user).isEmpty()) {
                throw new DomainException(
                        HttpStatus.CONFLICT,
                        "Active Session Found",
                        "login",
                        "Already logged in elsewhere"
                );
            }

            // 3. Generate JWT
            String token = jwt.generateToken(user.getPhoneNumber(), user.getRole().name());

            // 4. Store Session in Redis
            redis.saveSessionToRedis(token, user, ipAddress, userAgent);

            // 5. Create HttpOnly Cookie
            cookie.setHttpOnlyCookie(response, token);

            // --- AUDIT SUCCESS ---
            auditService.record(
                    user.getId(),
                    user.getPhoneNumber(),
                    user.getEmail(),
                    "LOGIN_SUCCESS",
                    ipAddress,
                    userAgent,
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
            // --- AUDIT FAILURE ---
            // We record the failure even if the user doesn't exist (userId will be null)
            auditService.record(
                    null,
                    isEmailInput ? null : identifier,
                    isEmailInput ? identifier : null,
                    "LOGIN_FAILURE",
                    ipAddress,
                    userAgent,
                    e.getMessage() // Records why it failed (Invalid user, Wrong pass, etc.)
            );
            // Re-throw the exception so the GlobalExceptionHandler takes over
            throw e;
        }
    }

    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // 1. Extract Token from Cookie (Using your CookieUtils for cleaner code)
        String token = cookie.extractToken(request);

        if (token != null && !token.isBlank()) {
            // 2. FETCH context from Redis BEFORE deleting
            // We need the session data (userId, phone) to record WHO is logging out
            redis.getSessionDetails(token).ifPresent(session -> {

                // 3. Record the Logout in PostgreSQL Audit
                auditService.record(
                        session.getUserId(),
                        session.getPhoneNumber(),
                        null,
                        "LOGOUT",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"),
                        "User logged out successfully"
                );

                // 4. Remove from Redis (Port 7379)
                redis.delSessionToRedis(token);
            });
        }

        // 5. Overwrite Cookie with "Expired" status
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
        if (token == null || token.isEmpty()) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    "Security Alert",
                    "token",
                    "No session found"
            );
        }

        // 1. Validate JWT Signature & Expiration
        if (!jwt.validateToken(token)) {
            throw new DomainException(
                    HttpStatus.UNAUTHORIZED,
                    "Session Expired",
                    "token",
                    "Please login again"
            );
        }

        // 2. Validate Redis Session (Crucial for Logout/Single-Session logic)
        UserSession session = redis.getSessionDetails(token)
                .orElseThrow(() -> new DomainException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid Session",
                        "token",
                        "Session terminated"));

        // 3. Return the Identity context (Traceability)
        return UserContext.builder()
                .userId(session.getUserId())
                .phoneNumber(session.getPhoneNumber())
                .role(session.getRole())
                .build();
    }
}