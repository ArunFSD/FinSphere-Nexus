package com.finsphere.auth.service;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.CustomerProfile;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.UserRole;
import com.finsphere.auth.exception.DomainException;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.model.UserSession;
import com.finsphere.auth.repository.UserRepository;
import com.finsphere.auth.repository.UserSessionRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserSessionRepository sessionRepository; // Redis Repository
    private final JwtUtils jwtUtils;

    @Transactional
    public String registerUser(RegistrationRequest request) throws Exception {

        // Business Rule: Unique Identity Check (Heavyweight - DB Call)
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "Validation Failed",
                    "Phone number is already registered");
        }

        // Email Check (Only if email is provided, since it's optional)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST,
                        "Validation Failed",
                        "Email is already registered"
                );
            }
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
        // 1. Find user by Phone or Email (Identifier)
        User user = userRepository.findByIdentifier(loginRequest.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // 2. Validate Password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 3. Generate JWT
        String token = jwtUtils.generateToken(user.getPhoneNumber(), user.getRole().name());

        // 4. Store Session in Redis
        UserSession session = UserSession.builder()
                .sessionId(token)
                .phoneNumber(user.getPhoneNumber())
                .loginIp(ipAddress)
                .userAgent(userAgent)
                .role(user.getRole().name())
                .build();
        sessionRepository.save(session);

        // 5. Create HttpOnly Cookie
        Cookie cookie = new Cookie("fsn_auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in Production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);

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
            sessionRepository.deleteById(token);
        }

        // 3. Overwrite Cookie with "Expired" status
        Cookie cookie = new Cookie("fsn_auth_token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // This tells the browser to delete it immediately
        response.addCookie(cookie);

        // 4. Clear Spring Security Context
        SecurityContextHolder.clearContext();
    }
}