package com.finsphere.common.dto.chit;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * A member's slot in a chit scheme.
 *
 * @param id         enrollment id
 * @param planId     plan id
 * @param planName   plan name
 * @param userId     member user id
 * @param memberName member name from the user directory (when known)
 * @param slotNumber slot (ticket) number, 1..totalMembers
 * @param active     whether the enrollment is active
 * @param joinedAt   when the member joined
 */
public record ChitEnrollmentResponse(
        Long id,
        Long planId,
        String planName,
        Long userId,
        @Nullable String memberName,
        Integer slotNumber,
        boolean active,
        @Nullable Instant joinedAt) {
}
