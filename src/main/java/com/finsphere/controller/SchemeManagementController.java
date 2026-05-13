package com.finsphere.controller;

import com.finsphere.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/schemes")
@RequiredArgsConstructor
public class SchemeManagementController {

    private final SecurityUtils securityUtils;

    @GetMapping("/active")
    public String activePortfolios(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("title", "Active Schemes");
        // Pass the same attributes so the fragment works
        model.addAttribute("userName", securityUtils.getCurrentUserFullName());
        model.addAttribute("userRole", securityUtils.getCurrentUserRole());

        return "scheme-management/activePortfolios";
    }
}
