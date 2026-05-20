package com.finsphere.controller;

import com.finsphere.common.dto.ApiResponse;
import com.finsphere.common.model.ChitPlanDTO;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.web.client.SchemeFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/fsn/schemes")
@RequiredArgsConstructor
@Slf4j
public class SchemeManagementController {

    private final SecurityUtils securityUtils;
    private final SchemeFeignClient schemeClient;

    @GetMapping("/active")
    public String activePortfolios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            HttpServletRequest request) {

        log.info(">>>> [SSR_HIT] Loading Active Portfolios | Page: {}", page);

        try {
            ResponseEntity<ApiResponse<Page<ChitPlanDTO>>> response = schemeClient.getActivePlans(page, size);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Page<ChitPlanDTO> pageData = response.getBody().getData();
                model.addAttribute("plans", pageData.getContent());
                model.addAttribute("currentPage", pageData.getNumber());
                model.addAttribute("totalPages", pageData.getTotalPages());
                model.addAttribute("totalItems", pageData.getTotalElements());
            } else {
                model.addAttribute("plans", List.of());
                model.addAttribute("error", "Failed to sync portfolios.");
            }
        } catch (Exception e) {
            log.error("!!!! [SSR_ERROR] Connection to Finance Service failed: {}", e.getMessage());
            model.addAttribute("plans", List.of());
            model.addAttribute("error", "Service currently unreachable.");
        }

        // Standard Layout Attributes
        model.addAttribute("title", "Active Schemes");
        model.addAttribute("currentUri", "/schemes/active");
        model.addAttribute("userName", securityUtils.getCurrentUserFullName());
        model.addAttribute("userRole", securityUtils.getCurrentUserRole());

        return "scheme-management/activePortfolios";
    }
}
