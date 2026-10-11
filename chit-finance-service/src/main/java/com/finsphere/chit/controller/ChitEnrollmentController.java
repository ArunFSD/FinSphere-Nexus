package com.finsphere.chit.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.finsphere.chit.service.ChitEnrollmentService;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.common.validation.ValidationGroups;

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
@RequestMapping(ApiPaths.ENROLLMENTS)
@Tag(name = "Chit enrolments")
public class ChitEnrollmentController {

    private final ChitEnrollmentService enrollmentService;

    public ChitEnrollmentController(ChitEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "Enrol a member into a free slot (customers: themselves only)", description = """
            Books slot `slotNumber` of scheme `planId` for member `userId`. A member (CUSTOMER) may only book for \
            themselves (403 otherwise). 404 when the scheme or the member is unknown: a new account reaches this \
            service a few seconds after sign-up. 400 when the slot is outside 1 to totalMembers, 409 when the slot \
            is already taken or the scheme no longer accepts members.""")
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<ChitEnrollmentResponse>> join(
            @Validated(ValidationGroups.Sequence.class) @RequestBody EnrollmentRequest request) {
        ChitEnrollmentResponse enrollment = this.enrollmentService.enroll(request);
        return ApiResponse.created("Enrolled in slot " + enrollment.slotNumber() + " of " + enrollment.planName(),
                enrollment)
            .toResponseEntity();
    }

    @Operation(summary = "Slots held by the signed-in user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ChitEnrollmentResponse>>> mine() {
        Long userId = SecurityUtils.requireCurrentUser().userId();
        return ApiResponse.ok("Your enrolments", this.enrollmentService.enrollmentsOfUser(userId))
            .toResponseEntity();
    }

    @Operation(summary = "Slots held by a member (admin, or the member)")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<ApiResponse<List<ChitEnrollmentResponse>>> ofUser(@PathVariable Long userId) {
        return ApiResponse.ok("Member enrolments", this.enrollmentService.enrollmentsOfUser(userId))
            .toResponseEntity();
    }

    @Operation(summary = "Members of a scheme (admin)")
    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ChitEnrollmentResponse>>> ofPlan(@PathVariable Long planId) {
        return ApiResponse.ok("Scheme members", this.enrollmentService.enrollmentsOfPlan(planId))
            .toResponseEntity();
    }
}
