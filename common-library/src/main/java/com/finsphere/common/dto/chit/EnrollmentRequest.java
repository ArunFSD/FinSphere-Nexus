package com.finsphere.common.dto.chit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;

/**
 * Enrols a member into a free slot of a chit scheme. Customers may only enrol themselves.
 *
 * @param planId     plan id
 * @param userId     member user id
 * @param slotNumber slot (ticket) number, 1..totalMembers
 */
public record EnrollmentRequest(
        @NotNull(message = "Choose a scheme", groups = FirstOrder.class)
        Long planId,

        @NotNull(message = "Choose a member", groups = FirstOrder.class)
        Long userId,

        @NotNull(message = "Choose a slot", groups = FirstOrder.class)
        @Min(value = 1, message = "Slot numbers start at 1", groups = SecondOrder.class)
        Integer slotNumber) {
}
