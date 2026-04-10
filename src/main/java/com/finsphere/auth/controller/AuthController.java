package com.finsphere.auth.controller;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.model.UserContext;
import com.finsphere.auth.service.AuthService;
import com.finsphere.auth.util.CookieUtils;
import com.finsphere.auth.validation.ValidationGroups;
import com.finsphere.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookie;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Validated(ValidationGroups.Sequence.class)
            @RequestBody RegistrationRequest regRequest,
            HttpServletRequest request) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return new ResponseEntity<>(authService.registerUser(regRequest, ip, ua), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        return ResponseEntity.ok(authService.login(loginRequest, ip, ua, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        return ResponseEntity.ok(authService.logout(request, response));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserContext>> validateToken(HttpServletRequest request) {

        // 1. Extract token from Cookie
        String token = cookie.extractToken(request);

        // 2. Business Logic: Check JWT and Redis (handled in service)
        UserContext identity = authService.validateSession(token);

        return ResponseEntity.ok(ApiResponse.<UserContext>builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("Session is valid")
                .data(identity)
                .build());
    }
}