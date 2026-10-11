package com.finsphere.auth.session;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.finsphere.auth.config.AuthProperties;
import com.finsphere.common.exception.DomainException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Brute-force protection: after {@code finsphere.auth.login.max-failed-attempts} wrong passwords an identifier
 * is locked for {@code lockout-duration}. Counters live in Redis (shared by all auth-service instances) and are
 * keyed by a hash of the identifier, never the phone number or e-mail itself.
 */
@Component
public class LoginAttemptService {

    private static final String KEY_PREFIX = "fsn:login-failures:";

    static final RedisScript<Long> INCREMENT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if redis.call('PTTL', KEYS[1]) < 0 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    private final int maxFailedAttempts;

    private final Duration lockoutDuration;

    public LoginAttemptService(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.maxFailedAttempts = properties.login().maxFailedAttempts();
        this.lockoutDuration = properties.login().lockoutDuration();
    }

    /** Throws HTTP 429 while the identifier is locked. */
    public void assertNotLocked(String identifier) {
        String key = key(identifier);
        String failures = this.redis.opsForValue().get(key);
        if (failures != null && parse(failures) >= this.maxFailedAttempts) {
            Long secondsLeft = this.redis.getExpire(key, TimeUnit.SECONDS);
            long seconds = (secondsLeft == null || secondsLeft < 0) ? this.lockoutDuration.toSeconds() : secondsLeft;
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed sign-in attempts.", "identifier",
                    "Sign-in is paused for this account. Try again in " + minutes
                            + ((minutes == 1) ? " minute." : " minutes."));
        }
    }

    /** @return failed attempts left before the lock applies */
    public long recordFailure(String identifier) {
        Long failures = this.redis.execute(INCREMENT, List.of(key(identifier)),
                String.valueOf(this.lockoutDuration.toMillis()));
        long count = (failures != null) ? failures : 0;
        return Math.max(0, this.maxFailedAttempts - count);
    }

    public void reset(String identifier) {
        this.redis.delete(key(identifier));
    }

    public Duration lockoutDuration() {
        return this.lockoutDuration;
    }

    private static String key(String identifier) {
        return KEY_PREFIX + TokenHasher.sha256(identifier.toLowerCase(Locale.ROOT));
    }

    private static long parse(String value) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ex) {
            return 0;
        }
    }
}
