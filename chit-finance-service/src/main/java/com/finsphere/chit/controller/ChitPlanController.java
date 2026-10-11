package com.finsphere.chit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.ChitPlanService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.validation.ValidationGroups;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.PLANS)
@Tag(name = "Chit schemes")
public class ChitPlanController {

    private final ChitPlanService planService;

    public ChitPlanController(ChitPlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Create a scheme and generate its monthly cycles (admin)", description = """
            Creates the scheme and its ledger: one month per `durationMonths`, starting at `startDate`, each with a \
            payment window of `paymentDuration` days (1 to 28). 400 when a value is missing or out of range \
            (`errors` names the field), or when `maxBidLimit` is above `totalValue`.""")
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChitPlanResponse>> create(
            @Validated(ValidationGroups.Sequence.class) @RequestBody ChitPlanRequest request) {
        ChitPlanResponse plan = this.planService.createPlan(request);
        return ApiResponse.created("Scheme created successfully", plan).toResponseEntity();
    }

    @Operation(summary = "Active schemes, newest start date first")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PageResponse<ChitPlanResponse>>> active(
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ChitPlanResponse> page = this.planService.activePlans(pageable);
        return ApiResponse.ok("Active schemes", page).toResponseEntity();
    }

    @Operation(summary = "Scheme with its monthly ledger and members", description = """
            Administrators see every slot holder. Members see the taken slot numbers and only their own \
            enrolments. 404 when there is no scheme with that id.""")
    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<ChitPlanDetailResponse>> detail(@PathVariable Long planId) {
        return ApiResponse.ok("Scheme details", this.planService.planDetail(planId)).toResponseEntity();
    }
}
