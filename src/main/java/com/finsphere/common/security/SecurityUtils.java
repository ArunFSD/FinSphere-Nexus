package com.finsphere.common.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder; // This will now resolve
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final JwtUtils jwtUtils;

    /**
     * Extracts the full Claims object from the current security context.
     * This assumes your JwtAuthenticationFilter sets the token in the credentials.
     */
    private Claims getClaims() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }
        String token = (String) authentication.getCredentials();
        return jwtUtils.getAllClaimsFromToken(token);
    }

    public Long getCurrentUserId() {
        Claims claims = getClaims();
        return claims != null ? claims.get("userId", Long.class) : null;
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