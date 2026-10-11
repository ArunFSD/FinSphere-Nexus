package com.finsphere.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.finsphere.auth.service.AuthService;
import com.finsphere.auth.service.LoginResult;
import com.finsphere.common.api.ApiResponse;
import com.finsphere.common.dto.auth.LoginRequest;
import com.finsphere.common.dto.auth.LoginResponse;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserContext;
import com.finsphere.common.dto.auth.UserProfileResponse;
import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.validation.ValidationGroups;
import com.finsphere.common.web.ClientInfo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API. The session JWT is only ever exchanged as an HttpOnly cookie.
 * <p>
 * The Swagger annotations describe the answers specific to each operation. The general ones (400, 401, 403, 404,
 * 409, 500, 503) are added to every operation by common-library's {@code ApiDocsCustomizer}; operations marked with
 * an empty {@link SecurityRequirements} are public.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, sign in, sign out and session checks")
public class AuthController {

    private static final String ERROR = "#/components/schemas/ApiError";

    private final AuthService authService;

    private final AuthCookieService cookieService;

    public AuthController(AuthService authService, AuthCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Operation(summary = "Register a new account", description = """
            Public. Creates a member (CUSTOMER) account that can sign in at once. Creating an ADMIN account needs \
            the session of an administrator, except for the very first administrator of a new database. The mobile \
            number has 10 digits starting with 6 to 9 (an optional +91 prefix is dropped), the password 8 to 30 \
            characters with letters and numbers.""")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created",
            useReturnTypeSchema = true)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "role is ADMIN, an administrator already exists and the caller is not one",
            content = @Content(schema = @Schema(ref = ERROR)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "The mobile number or e-mail already has an account (errors names the field)",
            content = @Content(schema = @Schema(ref = ERROR)))
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Validated(ValidationGroups.Sequence.class) @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {
        UserProfileResponse profile = this.authService.register(request, ClientInfo.from(httpRequest));
        return ApiResponse.created("Account successfully created", profile).toResponseEntity();
    }

    @Operation(summary = "Sign in with mobile number or e-mail, which sets the session cookie", description = """
            Public. On success the answer carries `Set-Cookie: fsn_auth_token=...; HttpOnly; SameSite=Lax`, and the \
            browser sends it with every later request to this host. Signing in again signs out older sessions of the \
            same account (concurrent-session-policy REPLACE).""")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Signed in",
            useReturnTypeSchema = true, headers = @Header(name = "Set-Cookie",
                    description = "fsn_auth_token=<session JWT>; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Wrong mobile number, e-mail or password. errors.password says how many attempts are left.",
            content = @Content(schema = @Schema(ref = ERROR)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "The account is disabled", content = @Content(schema = @Schema(ref = ERROR)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "Signed in on another device while concurrent-session-policy is REJECT",
            content = @Content(schema = @Schema(ref = ERROR)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429",
            description = "Too many wrong passwords: sign-in is paused for this account (15 minutes by default)",
            content = @Content(schema = @Schema(ref = ERROR)))
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LoginResult result = this.authService.login(request, ClientInfo.from(httpRequest));
        this.cookieService.writeToken(httpResponse, result.token());
        return ApiResponse.ok("Login successful", result.response()).toResponseEntity();
    }

    @Operation(summary = "Sign out, which revokes the session and clears the cookie",
            description = "Public and always 200, so a client can always clear its cookie.")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Signed out (also when the session had already ended)", useReturnTypeSchema = true,
            headers = @Header(name = "Set-Cookie", description = "fsn_auth_token=; Max-Age=0"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        this.authService.logout(this.cookieService.resolveToken(httpRequest), ClientInfo.from(httpRequest));
        this.cookieService.clear(httpResponse);
        SecurityContextHolder.clearContext();
        return ApiResponse.<Void>ok("Logged out successfully", null).toResponseEntity();
    }

    @Operation(summary = "Check that the session cookie belongs to a live session", description = """
            Public. Checks the signature, the expiry and that the session was not signed out or replaced by a newer \
            sign-in (Redis).""")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The session is live",
            useReturnTypeSchema = true)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "No cookie, or the session expired, was signed out or was replaced",
            content = @Content(schema = @Schema(ref = ERROR)))
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserContext>> validate(HttpServletRequest httpRequest) {
        UserContext context = this.authService.validateSession(this.cookieService.resolveToken(httpRequest));
        return ApiResponse.ok("Session is valid", context).toResponseEntity();
    }

    @Operation(summary = "Profile of the signed-in user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The profile",
            useReturnTypeSchema = true)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(HttpServletRequest httpRequest) {
        UserProfileResponse profile = this.authService.currentProfile(this.cookieService.resolveToken(httpRequest));
        return ApiResponse.ok("Current user", profile).toResponseEntity();
    }
}
