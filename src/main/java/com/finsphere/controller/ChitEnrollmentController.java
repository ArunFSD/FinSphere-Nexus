package com.finsphere.controller;

import com.finsphere.dto.EnrollmentRequest;
import com.finsphere.entity.ChitEnrollment;
import com.finsphere.service.ChitEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class ChitEnrollmentController {

    private final ChitEnrollmentService enrollmentService;

    @PostMapping("/join")
    public ResponseEntity<ChitEnrollment> enroll(@RequestBody EnrollmentRequest request) {
        return ResponseEntity.ok(enrollmentService.enrollUser(request));
    }

}
