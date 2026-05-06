package com.finsphere.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {

    @GetMapping("/{role}/dashboard")
    public String showDashboard(@PathVariable String role, Model model) {
        model.addAttribute("userRole", role.toUpperCase());
        return "dashboard"; // Points to dashboard.html
    }

}
