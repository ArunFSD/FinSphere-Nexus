package com.finsphere.frontend.web;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.LoginResponse;
import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.frontend.client.AuthClient;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.BackendUnavailableException;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.web.form.RegistrationForm;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.SafeRedirect;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sign in, sign up, sign out and password help. The session cookie is issued by auth-service; this controller only
 * passes it on to the browser (and clears it on sign-out).
 */
@Controller
public class AuthPageController {

    private static final Logger log = LoggerFactory.getLogger(AuthPageController.class);

    private static final String LOGIN = "auth/login";

    private static final String REGISTER = "auth/register";

    private static final String LOGOUT = "auth/logout";

    private final AuthClient authClient;

    private final AuthCookieService cookieService;

    private final FrontendProperties properties;

    private final CsrfTokenRepository csrfTokens;

    public AuthPageController(AuthClient authClient, AuthCookieService cookieService,
            FrontendProperties properties, CsrfTokenRepository csrfTokens) {
        this.authClient = authClient;
        this.cookieService = cookieService;
        this.properties = properties;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/")
    public String home() {
        return SecurityUtils.currentUser().isPresent() ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "next", required = false) @Nullable String next, Model model) {
        if (SecurityUtils.currentUser().isPresent()) {
            return "redirect:" + SafeRedirect.localPathOrHome(next);
        }
        model.addAttribute("identifier", "");
        model.addAttribute("next", SafeRedirect.localPathOrHome(next));
        return LOGIN;
    }

    @PostMapping("/login")
    public String login(@RequestParam(name = "identifier", defaultValue = "") String identifier,
            @RequestParam(name = "password", defaultValue = "") String password,
            @RequestParam(name = "next", required = false) @Nullable String next, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        String target = SafeRedirect.localPathOrHome(next);
        model.addAttribute("identifier", identifier.trim());
        model.addAttribute("next", target);
        if (identifier.isBlank() || password.isEmpty()) {
            model.addAttribute("error", "Enter your mobile number or e-mail and your password.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return LOGIN;
        }
        try {
            ResponseEntity<ApiResponse<LoginResponse>> result = this.authClient
                .login(new LoginRequest(identifier.trim(), password));
            copySetCookies(result, response);
            renewCsrfToken(request, response);
            return "redirect:" + target;
        }
        catch (BackendException ex) {
            if (ex.getStatus() >= 500) {
                // auth-service runs but cannot finish (its database or Redis is down, or it timed out)
                log.warn("Sign-in failed in auth-service: {} {}", ex.getStatus(), ex.getMessage());
                return signInUnavailable(model, response);
            }
            model.addAttribute("error", ex.userMessage());
            response.setStatus(ex.getStatus());
            return LOGIN;
        }
        catch (BackendUnavailableException ex) {
            log.warn("Sign-in unavailable: {}", ex.getMessage());
            return signInUnavailable(model, response);
        }
    }

    private static String signInUnavailable(Model model, HttpServletResponse response) {
        model.addAttribute("error", "Signing in is not available right now. Try again in a minute.");
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return LOGIN;
    }

    /**
     * A typed or bookmarked sign-out address. Signing out changes state, so it is never done by a GET: signed-in
     * visitors are asked to confirm (the page holds the POST form), everyone else goes to the sign-in page.
     */
    @GetMapping("/logout")
    public String logoutPage() {
        return SecurityUtils.currentUser().isPresent() ? LOGOUT : "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        if (SecurityUtils.currentUser().isPresent()) {
            try {
                copySetCookies(this.authClient.logout(), response);
            }
            catch (BackendException | BackendUnavailableException ex) {
                // The session is ended here either way, and auth-service expires its record on its own.
                log.warn("Sign-out could not reach auth-service: {}", ex.getMessage());
            }
        }
        this.cookieService.clear(response);
        renewCsrfToken(request, response);
        return "redirect:/login?notice=" + Notice.SIGNED_OUT.key();
    }

    /**
     * Drops the CSRF token when the signed-in person changes, so a token known before signing in (or planted by
     * someone else) is useless afterwards. The next page issues a new one.
     */
    private void renewCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        this.csrfTokens.saveToken(null, request, response);
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (SecurityUtils.currentUser().isPresent()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", RegistrationForm.empty("CUSTOMER"));
        model.addAttribute("errors", Map.of());
        return REGISTER;
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") @Nullable RegistrationForm submitted, Model model,
            HttpServletResponse response) {
        RegistrationForm form = (submitted != null) ? submitted : RegistrationForm.empty("CUSTOMER");
        model.addAttribute("form", form.withoutPasswords());
        if (!form.passwordsMatch()) {
            model.addAttribute("errors", Map.of("confirmPassword", "The two passwords do not match"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return REGISTER;
        }
        try {
            this.authClient.register(form.toRequest("CUSTOMER"));
            return "redirect:/login?notice=" + Notice.REGISTERED.key();
        }
        catch (BackendException ex) {
            if (ex.is(HttpStatus.UNAUTHORIZED) || ex.getStatus() >= 500) {
                throw ex;
            }
            model.addAttribute("errors", ex.getFieldErrors());
            model.addAttribute("error", ex.userMessage());
            response.setStatus(ex.getStatus());
            return REGISTER;
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("supportContact", this.properties.supportContact());
        return "auth/forgot-password";
    }

    static void copySetCookies(ResponseEntity<?> backendResponse, HttpServletResponse response) {
        List<String> cookies = backendResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            cookies.forEach((cookie) -> response.addHeader(HttpHeaders.SET_COOKIE, cookie));
        }
    }
}
