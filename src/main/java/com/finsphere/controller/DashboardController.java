package com.finsphere.controller;

import com.finsphere.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        String role = securityUtils.getCurrentUserRole();
        log.info(">>>> [DASHBOARD_ACCESS] Role detected: {}", role);

        if (role == null) {
            log.warn("!!!! [AUTH_FAILURE] No role found in context, redirecting to login");
            return "redirect:/login";
        }

        // Use your new AES-protected Masked ID for the UI
        model.addAttribute("userRole", role.toUpperCase());
        model.addAttribute("maskedUserId", securityUtils.getCurrentUserMaskedId());
        model.addAttribute("userName", securityUtils.getCurrentUserFullName());

        return "dashboard";
    }
}
