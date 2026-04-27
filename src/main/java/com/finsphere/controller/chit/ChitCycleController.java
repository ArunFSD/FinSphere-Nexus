package com.finsphere.controller.chit;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.AuctionRequest;
import com.finsphere.entity.chit.ChitMonthlyCycle;
import com.finsphere.service.chit.AuctionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.CYCLES)
@RequiredArgsConstructor
@Slf4j
public class ChitCycleController {

    private final AuctionService auctionService;

    @PostMapping("/plans/{planId}/month/{monthCount}/auction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitMonthlyCycle>> conductAuction(
            @PathVariable Long planId,
            @PathVariable Integer monthCount,
            @RequestBody AuctionRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [CHIT_API_HIT] POST /chits/cycles/plans/{}/month/{}/auction | IP: {} | Winner: {} | Bid: {}",
                planId, monthCount, httpServletRequest.getRemoteAddr(),
                request.getWinnerUserId(), request.getBidAmount());

        return ResponseEntity.ok(auctionService.processAuction(planId, monthCount, request));
    }
}
