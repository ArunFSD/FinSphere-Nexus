package com.finsphere.frontend.client;

import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.LoginResponse;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserProfileResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** auth-service. Sign-in and sign-out return the response so its session cookie can be passed to the browser. */
@HttpExchange(url = "/auth", accept = MediaType.APPLICATION_JSON_VALUE)
public interface AuthClient {

    @PostExchange(url = "/login", contentType = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request);

    @PostExchange(url = "/register", contentType = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserProfileResponse> register(@RequestBody RegistrationRequest request);

    @PostExchange("/logout")
    ResponseEntity<ApiResponse<Void>> logout();

    @GetExchange("/me")
    ApiResponse<UserProfileResponse> me();
}
