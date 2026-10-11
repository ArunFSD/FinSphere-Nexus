package com.finsphere.common.dto.auth;

/**
 * Result of a successful login. The JWT itself travels only in the HttpOnly session cookie.
 *
 * @param userId   user id
 * @param fullName display name
 * @param role     {@code CUSTOMER} or {@code ADMIN}
 */
public record LoginResponse(Long userId, String fullName, String role) {
}
