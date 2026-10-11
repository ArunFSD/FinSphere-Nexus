package com.finsphere.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings shared by every service that issues or verifies session tokens.
 *
 * @param secret     HMAC-SHA256 signing secret, at least 32 bytes (set {@code FSN_JWT_SECRET})
 * @param expiration token and session lifetime
 * @param issuer     expected {@code iss} claim
 */
@ConfigurationProperties("finsphere.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("24h") Duration expiration,
        @DefaultValue("finsphere-nexus") String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "finsphere.security.jwt.secret must be at least 32 bytes (256 bits). Set FSN_JWT_SECRET.");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("finsphere.security.jwt.expiration must be a positive duration");
        }
    }
}
