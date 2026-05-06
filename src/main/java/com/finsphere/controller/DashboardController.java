package com.finsphere.controller;

import com.finsphere.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SecurityUtils securityUtils; // Use your common lib

    @GetMapping("/dashboard") // FIXED: Removed /{role}
    public String showDashboard(Model model) {
        String role = securityUtils.getCurrentUserRole();
        String fullName = securityUtils.getCurrentUserFullName();

        if (role == null) {
            return "redirect:/login";
        }

        model.addAttribute("userRole", role.toUpperCase());
        model.addAttribute("userName", fullName);
        return "dashboard";
    }
}
