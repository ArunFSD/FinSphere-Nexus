package com.finsphere.common.dto.auth;

/**
 * Identity attached to a live session, returned by {@code GET /auth/validate}.
 *
 * @param userId      user id
 * @param phoneNumber login phone number
 * @param role        {@code CUSTOMER} or {@code ADMIN}
 */
public record UserContext(Long userId, String phoneNumber, String role) {
}
