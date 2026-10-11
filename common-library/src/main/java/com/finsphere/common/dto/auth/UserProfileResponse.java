package com.finsphere.common.dto.auth;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Public view of a user account and profile.
 *
 * @param userId      user id
 * @param fullName    display name
 * @param phoneNumber phone number
 * @param email       optional e-mail
 * @param careOf      optional C/O name
 * @param address     residential address
 * @param role        {@code CUSTOMER} or {@code ADMIN}
 * @param active      whether the account can sign in
 * @param memberSince account creation time
 */
public record UserProfileResponse(
        Long userId,
        String fullName,
        String phoneNumber,
        @Nullable String email,
        @Nullable String careOf,
        String address,
        String role,
        boolean active,
        @Nullable Instant memberSince) {
}
