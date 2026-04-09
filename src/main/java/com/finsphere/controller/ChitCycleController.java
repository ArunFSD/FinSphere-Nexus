package com.finsphere.controller;

import com.finsphere.dto.AuctionUpdateRequest;
import com.finsphere.entity.ChitMonthlyCycle;
import com.finsphere.service.ChitCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cycles")
@RequiredArgsConstructor
public class ChitCycleController {

    private final ChitCycleService cycleService;

    @PutMapping("/update-auction")
    public ResponseEntity<ChitMonthlyCycle> updateAuction(@RequestBody AuctionUpdateRequest request) {
        return ResponseEntity.ok(cycleService.updateAuctionResults(request));
    }
}
