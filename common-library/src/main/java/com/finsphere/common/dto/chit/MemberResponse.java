package com.finsphere.common.dto.chit;

import org.jspecify.annotations.Nullable;

/**
 * A member from chit-finance's copy of the user directory (used to pick members in admin forms).
 *
 * @param userId      user id
 * @param fullName    display name
 * @param phoneNumber phone number
 * @param careOf      optional C/O name
 * @param role        {@code CUSTOMER} or {@code ADMIN}
 */
public record MemberResponse(Long userId, String fullName, String phoneNumber, @Nullable String careOf,
        String role) {
}
