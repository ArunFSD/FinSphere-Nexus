package com.finsphere.web.controller;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.web.client.AuthFeignClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fsn")
@RequiredArgsConstructor
public class AuthProxyController {

    private final AuthFeignClient authClient;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegistrationRequest request) {
        try {
            return authClient.register(request);
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(null);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        try {
            return authClient.login(request);
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            return authClient.logout();
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(null);
        }
    }
}