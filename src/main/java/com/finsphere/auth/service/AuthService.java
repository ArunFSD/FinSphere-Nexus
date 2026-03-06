package com.finsphere.auth.service;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.CustomerProfile;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.UserRole;
import com.finsphere.auth.exception.DomainException;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.repository.UserRepository;
import com.finsphere.auth.security.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final CookieUtils cookie;
    private final RedisUtils redis;

    @Transactional
    public String registerUser(RegistrationRequest request) throws Exception {
        Map<String, String> businessErrors = new HashMap<>();

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            businessErrors.put("phoneNumber", "Phone number is already registered");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                businessErrors.put("email", "Email is already registered");
            }
        }

        if (!businessErrors.isEmpty()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Failed",
                    businessErrors
            );
        }

        // 1. Normalize phone BEFORE mapping
        String rawPhone = request.getPhoneNumber().replaceAll("[^0-9]", "");
        String normalizedPhone = rawPhone.substring(Math.max(0, rawPhone.length() - 10));
        request.setPhoneNumber(normalizedPhone);

        // 2. Map DTO to Entities using MapStruct
        User user = userMapper.toEntity(request);
        CustomerProfile profile = userMapper.toProfile(request);

        // 3. Manual Industrial Logic
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);

        // 4. Link them (Bi-directional link)
        user.setProfile(profile);
        profile.setUser(user);

        // 5. Save (CascadeType.ALL handles the profile automatically)
        userRepository.save(user);

        return "Registration successful for " + profile.getFullName();
    }

    public String login(LoginRequest loginRequest, String ipAddress, String userAgent, HttpServletResponse response)
            throws Exception {

        // 1. Find user by Phone or Email
        User user = userRepository.findByIdentifier(loginRequest.getIdentifier())
                .orElseThrow(() -> new DomainException(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "login", "Invalid phone number / email"
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
            throw new DomainException(HttpStatus.CONFLICT, "Active Session Found",
                    "login", "You are already logged in on another device. Please logout first.");
        }

        // 3. Generate JWT
        String token = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());

        // 4. Store Session in Redis
        redis.saveSessionToRedis(token, user, ipAddress, userAgent);

        // 5. Create HttpOnly Cookie
        cookie.setHttpOnlyCookie(response, token);

        return "Login successful for " + user.getPhoneNumber();
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 1. Extract Token from Cookie
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("fsn_auth_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Remove from Redis (Port 7379)
        if (token != null) {
            redis.delSessionToRedis(token);
        }

        // 3. Overwrite Cookie with "Expired" status
        cookie.delHttpOnlyCookie(response);

        // 4. Clear Spring Security Context
        SecurityContextHolder.clearContext();
    }
}