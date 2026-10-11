package com.finsphere.frontend.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.security.JwtAuthenticationFilter;
import com.finsphere.common.security.JwtService;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.SafeRedirect;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Pages are protected by the same session cookie the services use (validated by {@link JwtAuthenticationFilter}).
 * No server-side HTTP session exists: the CSRF token lives in the {@code XSRF-TOKEN} cookie and every form carries
 * it, so any front-end instance can serve any request.
 */
@Configuration(proxyBeanMethods = false)
public class FrontendSecurityConfig {

    /** Everything is served from this application: no inline scripts or styles, no third-party hosts. */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; form-action 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'; object-src 'none'";

    /** The {@code XSRF-TOKEN} cookie. Shared with sign-in and sign-out, which renew the token. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityFilterChain pageSecurity(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthCookieService cookieService, JwtService jwtService, CsrfTokenRepository csrfTokenRepository)
            throws Exception {
        http.csrf((csrf) -> csrf.spa()
                .csrfTokenRepository(csrfTokenRepository)
                // One token per browser for the whole session. With a stateless session policy Spring Security would
                // renew it on every signed-in request, which breaks any form open in a second tab. The token is
                // renewed when the signed-in person changes instead (AuthPageController).
                .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache((cache) -> cache.disable())
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests((auth) -> auth
                .requestMatchers("/css/**", "/js/**", "/fonts/**", "/img/**", "/favicon.svg", "/favicon.ico",
                        "/robots.txt")
                .permitAll()
                .requestMatchers("/", "/login", "/logout", "/register", "/forgot-password", "/error", "/error/**")
                .permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/actuator/**")
                .hasRole("ADMIN")
                .requestMatchers("/members", "/members/**", "/schemes/new", "/loans/new", "/payments/new")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/schemes", "/schemes/*/auction", "/loans", "/payments")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated())
            .exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(new SignInRedirect(cookieService))
                .accessDeniedHandler(new ExpiredFormOrForbidden(cookieService, jwtService)))
            .headers((headers) -> headers
                .contentSecurityPolicy((csp) -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy((referrer) -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** {@code /login}, saying whether the session ended, and naming the page to return to unless it is the overview. */
    static String signInAddress(boolean sessionEnded, String next) {
        StringBuilder target = new StringBuilder("/login");
        if (sessionEnded) {
            target.append("?notice=").append(Notice.SESSION_ENDED.key());
        }
        if (!SafeRedirect.HOME.equals(next)) {
            target.append(sessionEnded ? '&' : '?').append("next=")
                .append(URLEncoder.encode(next, StandardCharsets.UTF_8));
        }
        return target.toString();
    }

    /**
     * Visitors who are not signed in go to the sign-in page, which brings them back afterwards: to the page they
     * asked for, or (when a form was sent) to the page that held the form. A session cookie that is no longer valid
     * (expired, or signed with an old key) is removed, and the sign-in page says the session ended.
     */
    static final class SignInRedirect implements AuthenticationEntryPoint {

        private final AuthCookieService cookieService;

        SignInRedirect(AuthCookieService cookieService) {
            this.cookieService = cookieService;
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                AuthenticationException authException) throws IOException {
            boolean staleCookie = this.cookieService.resolveToken(request).isPresent();
            if (staleCookie) {
                this.cookieService.clear(response);
            }
            // Only a signed-in page holds the forms that reach here, so a form sent without a session means it ended
            boolean sessionEnded = staleCookie || !HttpMethod.GET.matches(request.getMethod());
            response.sendRedirect(request.getContextPath()
                    + signInAddress(sessionEnded, SafeRedirect.returnPath(request)));
        }
    }

    /**
     * A form sent with a missing or stale CSRF token (cookies cleared, or a page restored after the browser was
     * closed) goes back to the page that held it with a notice, so nothing is saved and nobody meets a bare error.
     * When the session has ended as well (signed out in another tab), the visitor signs in first and then returns to
     * that page. Anything else is a real permission problem and gets the 403 page.
     * <p>
     * The CSRF check runs before the session cookie is read, so the session is checked here from the cookie itself.
     */
    static final class ExpiredFormOrForbidden implements AccessDeniedHandler {

        private final AccessDeniedHandler forbidden = new AccessDeniedHandlerImpl();

        private final AuthCookieService cookieService;

        private final JwtService jwtService;

        ExpiredFormOrForbidden(AuthCookieService cookieService, JwtService jwtService) {
            this.cookieService = cookieService;
            this.jwtService = jwtService;
        }

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
                throws IOException, ServletException {
            if (!(denied instanceof CsrfException) || response.isCommitted()) {
                this.forbidden.handle(request, response, denied);
                return;
            }
            String path = SafeRedirect.pathWithinApplication(request);
            boolean publicForm = "/login".equals(path) || "/register".equals(path);
            String page = SafeRedirect.formPage(request);
            if (page == null) {
                page = publicForm ? path : SafeRedirect.HOME;
            }
            Optional<String> cookie = this.cookieService.resolveToken(request);
            boolean signedIn = cookie.flatMap(this.jwtService::parse).isPresent();
            String target;
            if (signedIn || publicForm) {
                target = SafeRedirect.withNotice(page, Notice.FORM_EXPIRED);
            }
            else if ("/logout".equals(path)) {
                // Signing out without a session: the visitor is signed out already
                target = "/login?notice=" + Notice.SIGNED_OUT.key();
            }
            else {
                if (cookie.isPresent()) {
                    this.cookieService.clear(response);
                }
                target = signInAddress(true, SafeRedirect.localPathOrHome(page));
            }
            response.sendRedirect(request.getContextPath() + target);
        }
    }
}
