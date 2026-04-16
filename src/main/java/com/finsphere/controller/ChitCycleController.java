package com.finsphere.controller;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.dto.AuctionRequest;
import com.finsphere.entity.ChitMonthlyCycle;
import com.finsphere.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cycles")
@RequiredArgsConstructor
public class ChitCycleController {

    private final AuctionService auctionService;

    @PostMapping("/plans/{planId}/cycles/{monthCount}/auction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitMonthlyCycle>> conductAuction(
            @PathVariable Long planId,
            @PathVariable Integer monthCount,
            @RequestBody AuctionRequest request) {

        return ResponseEntity.ok(auctionService.processAuction(planId, monthCount, request));
    }
}
