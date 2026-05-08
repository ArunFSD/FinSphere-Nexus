package com.finsphere.common.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final JwtUtils jwtUtils;
    private final EncryptionUtils encryptionUtils;

    private Claims getClaims() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }
        try {
            String token = (String) authentication.getCredentials();
            return jwtUtils.getAllClaimsFromToken(token);
        } catch (Exception e) {
            log.warn(">>>> [SECURITY_CONTEXT] Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }

    public Long getCurrentUserId() {
        Claims claims = getClaims();
        if (claims == null) return null;
        Object userId = claims.get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        return null;
    }

    public String getCurrentUserMaskedId() {
        Long rawId = getCurrentUserId();
        if (rawId == null) {
            log.warn(">>>> [MASKING] Cannot mask null UserID");
            return "ANONYMOUS";
        }
        return encryptionUtils.encrypt(rawId.toString());
    }

    public String getCurrentUserFullName() {
        Claims claims = getClaims();
        return claims != null ? claims.get("fullName", String.class) : "Guest";
    }

    public String getCurrentIdentifier() {
        Claims claims = getClaims();
        return claims != null ? claims.getSubject() : null;
    }

    public String getCurrentUserRole() {
        Claims claims = getClaims();
        return claims != null ? claims.get("role", String.class) : null;
    }
}