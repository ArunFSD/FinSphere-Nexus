package com.finsphere.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${finsphere.jwt.secret}")
    private String jwtSecret;

    @Value("${finsphere.jwt.expirationMs}")
    private int jwtExpirationsMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String identifier, String role) {
        log.debug(">>>> [JWT_GENERATE] Creating token for: {} | Role: {}", identifier, role);
        return Jwts.builder()
                .subject(identifier)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationsMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getIdentifierFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * NEW: Extracts the role claim so the Other Services can perform Role-Based Access Control
     */
    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("!!!! [JWT_VALIDATE_ERROR] Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}