package com.finsphere.web.client;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.RegistrationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:7080/auth")
public interface AuthFeignClient {

    @PostMapping("/register")
    ResponseEntity<ApiResponse<Void>> register(@RequestBody RegistrationRequest request);

    @PostMapping("/login")
    ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request);

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout();
}
