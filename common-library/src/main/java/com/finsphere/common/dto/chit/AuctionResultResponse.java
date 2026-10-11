package com.finsphere.common.dto.chit;

import java.math.BigDecimal;

/**
 * Outcome of a settled auction.
 *
 * @param cycle            the settled cycle
 * @param prizeMoney       amount paid out to the winner (chit value minus bid)
 * @param organiserCommission commission retained by the organiser
 * @param winNumber        which of the winner's slots this win used (1-based)
 * @param slotsHeld        slots the winner holds in this plan
 */
public record AuctionResultResponse(
        ChitCycleResponse cycle,
        BigDecimal prizeMoney,
        BigDecimal organiserCommission,
        long winNumber,
        long slotsHeld) {
}
