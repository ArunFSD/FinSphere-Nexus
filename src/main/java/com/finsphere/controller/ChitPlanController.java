package com.finsphere.controller;

import com.finsphere.dto.ChitPlanRequest;
import com.finsphere.entity.ChitPlan;
import com.finsphere.service.ChitPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class ChitPlanController {

    private final ChitPlanService planService;

    @PostMapping("/create")
    public ResponseEntity<ChitPlan> createPlan(@RequestBody ChitPlanRequest request) {
        return ResponseEntity.ok(planService.createPlan(request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ChitPlan>> getActivePlans() {
        return ResponseEntity.ok(planService.getAllActivePlans());
    }
}