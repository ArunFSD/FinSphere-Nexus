package com.finsphere.controller;

import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.RegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class UserInterfaceController {

    /**
     * Default Landing Page - Redirects to Login.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    /**
     * Serves the Login Page with an empty LoginRequest DTO.
     */
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        log.info(">>>> [UI_NAV] Navigating to Login Page");
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    /**
     * Serves the Registration Page with an empty RegistrationRequest DTO.
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        log.info(">>>> [UI_NAV] Navigating to Registration Page");
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    /**
     * Serves the Forgot Password Page (Static for now).
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        log.info(">>>> [UI_NAV] Navigating to Forgot Password");
        return "forgot-password";
    }
}