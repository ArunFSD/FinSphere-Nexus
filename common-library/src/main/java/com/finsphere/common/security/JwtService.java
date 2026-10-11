package com.finsphere.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Issues and verifies the HMAC-SHA256 session JWT.
 * <p>
 * The parser is built once (thread-safe) and a token is parsed once per request by
 * {@link JwtAuthenticationFilter}; everything downstream reads the resulting {@link AuthenticatedUser}.
 */
public class JwtService {

    static final String CLAIM_USER_ID = "userId";

    static final String CLAIM_ROLE = "role";

    static final String CLAIM_FULL_NAME = "fullName";

    static final String CLAIM_EMAIL = "email";

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;

    private final JwtProperties properties;

    private final Clock clock;

    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
            .verifyWith(this.key)
            .requireIssuer(properties.issuer())
            .clock(() -> Date.from(clock.instant()))
            .clockSkewSeconds(30)
            .build();
    }

    public String issue(AuthenticatedUser user) {
        Instant now = this.clock.instant();
        JwtBuilder builder = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuer(this.properties.issuer())
            .subject(user.identifier())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(this.properties.expiration())))
            .claim(CLAIM_USER_ID, user.userId())
            .claim(CLAIM_ROLE, user.role())
            .claim(CLAIM_FULL_NAME, user.fullName());
        if (user.email() != null && !user.email().isBlank()) {
            builder.claim(CLAIM_EMAIL, user.email());
        }
        return builder.signWith(this.key, Jwts.SIG.HS256).compact();
    }

    /**
     * Verifies signature, issuer and expiry.
     * @param token the compact JWT, may be {@code null}
     * @return the user, or empty when the token is missing, tampered with or expired
     */
    public Optional<AuthenticatedUser> parse(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = this.parser.parseSignedClaims(token).getPayload();
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            String subject = claims.getSubject();
            if (userId == null || role == null || subject == null) {
                log.debug("Rejected JWT without mandatory claims");
                return Optional.empty();
            }
            String fullName = Objects.requireNonNullElse(claims.get(CLAIM_FULL_NAME, String.class), "User");
            return Optional.of(new AuthenticatedUser(userId, subject, role, fullName,
                    claims.get(CLAIM_EMAIL, String.class)));
        }
        catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Duration expiration() {
        return this.properties.expiration();
    }
}
