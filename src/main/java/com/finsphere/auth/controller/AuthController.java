package com.finsphere.auth.controller;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.model.UserContext;
import com.finsphere.auth.service.AuthService;
import com.finsphere.auth.util.CookieUtils;
import com.finsphere.auth.validation.ValidationGroups;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookie;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Validated(ValidationGroups.Sequence.class)
            @RequestBody RegistrationRequest regRequest,
            HttpServletRequest request) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return ResponseEntity.ok(authService.registerUser(regRequest, ip, ua));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return ResponseEntity.ok(authService.login(loginRequest, ip, ua, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        authService.logout(request, response);
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/validate")
    public ResponseEntity<UserContext> validateToken(HttpServletRequest request) {
        // 1. Extract token from Cookie
        String token = cookie.extractToken(request);

        // 2. Business Logic: Check JWT and Redis (handled in service)
        UserContext identity = authService.validateSession(token);

        return ResponseEntity.ok(identity);
    }
}
