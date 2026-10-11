package com.finsphere.auth.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.JwtProperties;
import com.finsphere.common.web.ClientInfo;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Server-side session store in Redis, so a JWT can be revoked before it expires (logout, re-login elsewhere).
 * <p>
 * Layout per user (the {@code {u<id>}} hash tag keeps a user's keys in one Redis Cluster slot):
 * <ul>
 * <li>{@code fsn:{u42}:session:<sha256(token)>} hash with the session details, TTL = JWT lifetime</li>
 * <li>{@code fsn:{u42}:sessions} set of that user's session hashes, TTL = JWT lifetime</li>
 * </ul>
 * Multi-key changes run as Lua scripts so they are atomic and never leave a key without a TTL.
 */
@Component
public class SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    static final RedisScript<Long> REGISTER = RedisScript.of("""
            redis.call('HSET', KEYS[1], unpack(ARGV, 3))
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
            redis.call('SADD', KEYS[2], ARGV[2])
            redis.call('PEXPIRE', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    static final RedisScript<Long> REVOKE = RedisScript.of("""
            local removed = redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[1])
            return removed
            """, Long.class);

    static final RedisScript<Long> REVOKE_ALL = RedisScript.of("""
            local hashes = redis.call('SMEMBERS', KEYS[1])
            local removed = 0
            for _, hash in ipairs(hashes) do
              removed = removed + redis.call('DEL', ARGV[1] .. hash)
            end
            redis.call('DEL', KEYS[1])
            return removed
            """, Long.class);

    static final RedisScript<Long> COUNT_ACTIVE = RedisScript.of("""
            local hashes = redis.call('SMEMBERS', KEYS[1])
            local live = 0
            for _, hash in ipairs(hashes) do
              if redis.call('EXISTS', ARGV[1] .. hash) == 1 then
                live = live + 1
              else
                redis.call('SREM', KEYS[1], hash)
              end
            end
            return live
            """, Long.class);

    private final StringRedisTemplate redis;

    private final Duration ttl;

    private final Clock clock;

    public SessionRegistry(StringRedisTemplate redis, JwtProperties jwtProperties, Clock clock) {
        this.redis = redis;
        this.ttl = jwtProperties.expiration();
        this.clock = clock;
    }

    public void register(String token, AuthenticatedUser user, ClientInfo client) {
        String hash = TokenHasher.sha256(token);
        this.redis.execute(REGISTER, List.of(sessionKey(user.userId(), hash), userSessionsKey(user.userId())),
                String.valueOf(this.ttl.toMillis()), hash,
                "userId", String.valueOf(user.userId()),
                "phoneNumber", user.identifier(),
                "role", user.role(),
                "ipAddress", client.ipAddress(),
                "userAgent", client.userAgent(),
                "createdAt", this.clock.instant().toString());
    }

    public Optional<SessionInfo> find(Long userId, String token) {
        Map<Object, Object> fields = this.redis.opsForHash().entries(sessionKey(userId, TokenHasher.sha256(token)));
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        try {
            Long storedUserId = Long.valueOf(String.valueOf(fields.get("userId")));
            if (!Objects.equals(storedUserId, userId)) {
                return Optional.empty();
            }
            return Optional.of(new SessionInfo(storedUserId, String.valueOf(fields.get("phoneNumber")),
                    String.valueOf(fields.get("role")), String.valueOf(fields.get("ipAddress")),
                    String.valueOf(fields.get("userAgent")), Instant.parse(String.valueOf(fields.get("createdAt")))));
        }
        catch (RuntimeException ex) {
            log.warn("Ignoring malformed session entry for user {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    /** @return {@code true} when a live session was removed */
    public boolean revoke(Long userId, String token) {
        String hash = TokenHasher.sha256(token);
        Long removed = this.redis.execute(REVOKE, List.of(sessionKey(userId, hash), userSessionsKey(userId)), hash);
        return removed != null && removed > 0;
    }

    /** Signs the user out everywhere. @return number of sessions removed */
    public long revokeAll(Long userId) {
        Long removed = this.redis.execute(REVOKE_ALL, List.of(userSessionsKey(userId)), sessionKeyPrefix(userId));
        return (removed != null) ? removed : 0;
    }

    /** Counts live sessions and prunes references to expired ones. */
    public long activeSessions(Long userId) {
        Long live = this.redis.execute(COUNT_ACTIVE, List.of(userSessionsKey(userId)), sessionKeyPrefix(userId));
        return (live != null) ? live : 0;
    }

    static String sessionKeyPrefix(Long userId) {
        return "fsn:{u" + userId + "}:session:";
    }

    static String sessionKey(Long userId, String tokenHash) {
        return sessionKeyPrefix(userId) + tokenHash;
    }

    static String userSessionsKey(Long userId) {
        return "fsn:{u" + userId + "}:sessions";
    }
}
