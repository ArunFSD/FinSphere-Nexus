package com.finsphere.auth.controller;

import com.finsphere.auth.dto.LoginRequest;
import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.service.AuthService;
import com.finsphere.auth.util.CookieUtils;
import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.dto.UserContext;
import com.finsphere.common.validation.ValidationGroups;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
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

        log.info(">>>> [AUTH_API_HIT] Endpoint: /register | IP: {} | UA: {}", ip, ua);

        ApiResponse<Void> response = authService.registerUser(regRequest, ip, ua);

        log.info("<<<< [AUTH_API_RESPONSE] Status: {} | Flow: Registration | User: {}",
                response.getStatus(), regRequest.getPhoneNumber());

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");

        log.info(">>>> [AUTH_API_HIT] Endpoint: /login | IP: {} | UA: {}", ip, ua);

        ApiResponse<Map<String, String>> apiResponse = authService.login(loginRequest, ip, ua, response);

        log.info("<<<< [AUTH_API_RESPONSE] Status: {} | Flow: Login | Identifier: {}",
                apiResponse.getStatus(), loginRequest.getIdentifier());

        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String ip = request.getRemoteAddr();

        log.info(">>>> [AUTH_API_HIT] Endpoint: /logout | IP: {}", ip);

        ApiResponse<Void> apiResponse = authService.logout(request, response);

        log.info("<<<< [AUTH_API_RESPONSE] Status: {} | Flow: Logout", apiResponse.getStatus());

        return ResponseEntity.status(apiResponse.getStatus()).body(apiResponse);
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserContext>> validateToken(HttpServletRequest request) {

        log.info(">>>> [AUTH_API_HIT] Endpoint: /validate | IP: {}", request.getRemoteAddr());

        String token = cookie.extractToken(request);
        UserContext identity = authService.validateSession(token);

        ApiResponse<UserContext> response = ApiResponse.<UserContext>builder()
                .success(true)
                .status(200)
                .message("Session is valid")
                .data(identity)
                .build();

        log.info("<<<< [AUTH_API_RESPONSE] Status: 200 | Flow: Token Validation | UserID: {}",
                identity.getUserId());

        return ResponseEntity.ok(response);
    }
}