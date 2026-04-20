package com.finsphere.controller;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.dto.EnrollmentRequest;
import com.finsphere.service.ChitEnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Slf4j
public class ChitEnrollmentController {

    private final ChitEnrollmentService enrollmentService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> enroll(
            @RequestBody EnrollmentRequest request,
            HttpServletRequest httpServletRequest) {

        log.info(">>>> [CHIT_API_HIT] POST /chits/enrollments/join | IP: {} | User: {} | Plan: {}",
                httpServletRequest.getRemoteAddr(), request.getUserId(), request.getPlanId());

        ApiResponse<Void> response = enrollmentService.enrollUser(
                request.getPlanId(),
                request.getUserId(),
                request.getSlotNumber()
        );

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
