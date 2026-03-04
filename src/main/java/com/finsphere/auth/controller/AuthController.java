package com.finsphere.auth.controller;

import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.UserRole;
import com.finsphere.auth.repository.UserRepository;
import com.finsphere.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) throws Exception {
        return ResponseEntity.ok(authService.registerUser(request));
    }
}
