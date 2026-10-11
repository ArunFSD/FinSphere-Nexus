package com.finsphere.common.event;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Published by auth-service on {@link KafkaTopics#USER_UPDATES} whenever a user is created or changed, so other
 * services can keep a local read-only copy of the user directory.
 *
 * @param userId      user id (also the Kafka record key, keeping a user's events ordered)
 * @param fullName    display name
 * @param phoneNumber phone number
 * @param careOf      optional C/O name
 * @param role        {@code CUSTOMER} or {@code ADMIN}
 * @param active      whether the account is active
 * @param occurredAt  when the change happened
 */
public record UserUpdateEvent(
        Long userId,
        String fullName,
        String phoneNumber,
        @Nullable String careOf,
        String role,
        boolean active,
        Instant occurredAt) {
}
