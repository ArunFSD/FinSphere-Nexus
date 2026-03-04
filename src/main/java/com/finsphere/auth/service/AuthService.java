package com.finsphere.auth.service;

import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.CustomerProfile;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.UserRole;
import com.finsphere.auth.mapper.UserMapper;
import com.finsphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public String registerUser(RegistrationRequest request) throws Exception{
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
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
}