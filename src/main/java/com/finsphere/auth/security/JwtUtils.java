package com.finsphere.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${finsphere.jwt.secret}")
    private String jwtSecret;

    @Value("${finsphere.jwt.expirationMs}")
    private int jwtExpirationsMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String identifier, String role) {
        return Jwts.builder()
                .subject(identifier) // Phone or Email
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

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parse(token);
            return true;
        } catch (Exception e) {
            // Log this for Loki later
            return false;
        }
    }
}