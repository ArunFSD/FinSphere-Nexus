package com.finsphere.controller.chit;


import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.validation.ValidationGroups;
import com.finsphere.constansts.ApiConstants;
import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.chit.ChitPlan;
import com.finsphere.service.chit.ChitPlanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.PLANS)
@RequiredArgsConstructor
@Slf4j
public class ChitPlanController {

    private final ChitPlanService planService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitPlan>> createPlan(
            @Validated(ValidationGroups.Sequence.class)
            @RequestBody ChitPlanRequest request,
            HttpServletRequest httpServletRequest
    ) throws Exception {
        log.info(">>>> [CHIT_API_HIT] POST /chits/plans/create | IP: {}", httpServletRequest.getRemoteAddr());
        ApiResponse<ChitPlan> response = planService.createPlan(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<ChitPlan>>> getActivePlans(
            @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest request
    ) throws Exception {
        log.info(">>>> [CHIT_ACTIVE] GET /chits/plans/active | IP: {}", request.getRemoteAddr());
        ApiResponse<Page<ChitPlan>> response = planService.getAllActivePlans(pageable);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}