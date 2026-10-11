package com.finsphere.common.dto.chit;

import java.util.List;

/**
 * A chit scheme with its monthly ledger and its members.
 *
 * @param plan        the plan
 * @param cycles      monthly cycles in month order
 * @param enrollments active enrollments in slot order (customers only see their own)
 * @param takenSlots  every slot number currently held by an active member, without saying who holds it, so a
 *                    customer can pick a free slot
 */
public record ChitPlanDetailResponse(
        ChitPlanResponse plan,
        List<ChitCycleResponse> cycles,
        List<ChitEnrollmentResponse> enrollments,
        List<Integer> takenSlots) {

    public ChitPlanDetailResponse {
        cycles = (cycles != null) ? List.copyOf(cycles) : List.of();
        enrollments = (enrollments != null) ? List.copyOf(enrollments) : List.of();
        takenSlots = (takenSlots != null) ? List.copyOf(takenSlots) : List.of();
    }
}
