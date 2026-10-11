package com.finsphere.chit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.AuctionService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.format.Rupees;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.AuctionResultResponse;
import com.finsphere.common.validation.ValidationGroups;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CYCLES)
@Tag(name = "Monthly auctions")
public class ChitCycleController {

    private final AuctionService auctionService;

    public ChitCycleController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Operation(summary = "Settle a month's auction (admin)", description = """
            Closes month `monthlyCount` of scheme `planId`. The winner (`winnerUserId`) receives the chit value \
            minus the bid, the bid minus the commission is shared out as a dividend, and every member's payable for \
            the month drops by that dividend. 404 when the scheme has no such month, 409 when the month is already \
            settled or the member has already won once for every slot they hold, 400 when the bid is above the \
            scheme's limit or the member is not in the scheme.""")
    @PostMapping("/auction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuctionResultResponse>> auction(
            @Validated(ValidationGroups.Sequence.class) @RequestBody AuctionRequest request) {
        AuctionResultResponse result = this.auctionService.settle(request);
        String message = "Auction settled. The winner receives " + Rupees.format(result.prizeMoney()) + ".";
        return ApiResponse.ok(message, result)
            .toResponseEntity();
    }
}
