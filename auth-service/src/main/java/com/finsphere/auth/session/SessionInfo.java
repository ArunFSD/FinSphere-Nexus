package com.finsphere.auth.session;

import java.time.Instant;

/**
 * A live session as stored in Redis.
 *
 * @param userId      user id
 * @param phoneNumber login phone number
 * @param role        role at sign-in
 * @param ipAddress   client IP at sign-in
 * @param userAgent   client user agent at sign-in
 * @param createdAt   sign-in time
 */
public record SessionInfo(Long userId, String phoneNumber, String role, String ipAddress, String userAgent,
        Instant createdAt) {
}
