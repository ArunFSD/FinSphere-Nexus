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

        ApiResponse<Void> response = authService.registerUser(regRequest, ip, ua);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        ApiResponse<Map<String, String>> apiResponse = authService.login(loginRequest, ip, ua, response);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        ApiResponse<Void> apiResponse = authService.logout(request, response);
        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserContext>> validateToken(HttpServletRequest request) {
        String token = cookie.extractToken(request);
        UserContext identity = authService.validateSession(token);

        ApiResponse<UserContext> response = ApiResponse.<UserContext>builder()
                .success(true)
                .status(200)
                .message("Session is valid")
                .data(identity)
                .build();

        return ResponseEntity.ok(response);
    }
}