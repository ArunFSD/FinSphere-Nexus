package com.finsphere.controller.chit;


import com.finsphere.common.dto.ApiResponse;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.service.chit.ChitPlanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.PLANS) // Base mapping for the whole project
@RequiredArgsConstructor
@Slf4j
public class ChitPlanController {

    private final ChitPlanService planService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitPlan>> createPlan(
            @RequestBody ChitPlanRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [CHIT_API_HIT] POST /chits/plans/create | IP: {}", httpServletRequest.getRemoteAddr());
        ApiResponse<ChitPlan> response = planService.createPlan(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChitPlan>>> getActivePlans(HttpServletRequest httpServletRequest) {
        log.info(">>>> [CHIT_API_HIT] GET /chits/plans/active | IP: {}", httpServletRequest.getRemoteAddr());
        ApiResponse<List<ChitPlan>> response = planService.getAllActivePlans();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}