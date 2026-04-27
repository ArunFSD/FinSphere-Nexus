package com.finsphere.controller.chit;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.validation.ValidationGroups;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.AuctionRequest;
import com.finsphere.entity.chit.ChitMonthlyCycle;
import com.finsphere.service.chit.AuctionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.CYCLES)
@RequiredArgsConstructor
@Slf4j
public class ChitCycleController {

    private final AuctionService auctionService;

    @PostMapping("/auction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitMonthlyCycle>> conductAuction(
            @Validated(ValidationGroups.Sequence.class)
            @RequestBody AuctionRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [CHIT_API_HIT] POST /chits/cycles/auction | IP: {} | Winner: {} | Bid: {}",
                httpServletRequest.getRemoteAddr(), request.getWinnerUserId(), request.getBidAmount());

        ApiResponse<ChitMonthlyCycle> response = auctionService.processAuction(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
