package com.finsphere.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * auth-service settings ({@code finsphere.auth.*}).
 *
 * @param concurrentSessionPolicy what happens when a user signs in while another session is still active
 * @param login                   brute-force protection
 */
@ConfigurationProperties("finsphere.auth")
public record AuthProperties(
        @DefaultValue("REPLACE") ConcurrentSessionPolicy concurrentSessionPolicy,
        @DefaultValue Login login) {

    public enum ConcurrentSessionPolicy {

        /** The new sign-in wins: older sessions are revoked (the user is never locked out). */
        REPLACE,

        /** The new sign-in is rejected with 409 while another session is active. */
        REJECT
    }

    /**
     * @param maxFailedAttempts wrong passwords allowed before the identifier is locked
     * @param lockoutDuration   how long the lock lasts
     */
    public record Login(
            @DefaultValue("5") int maxFailedAttempts,
            @DefaultValue("15m") Duration lockoutDuration) {
    }
}
