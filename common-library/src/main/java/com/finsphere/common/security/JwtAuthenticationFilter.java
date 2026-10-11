package com.finsphere.common.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests from the session cookie.
 * <p>
 * The JWT is verified once here and the resulting {@link AuthenticatedUser} becomes the principal. A missing or
 * invalid token leaves the request anonymous; the security rules of each service then decide between public
 * access and a 401. This filter is added to the Spring Security chain explicitly and is <em>not</em> registered
 * as a servlet filter on its own (see the auto-configuration).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String MDC_USER_ID = "userId";

    private static final List<String> STATIC_PREFIXES = List.of("/css/", "/js/", "/images/", "/img/", "/webjars/",
            "/fonts/", "/favicon");

    private final JwtService jwtService;

    private final AuthCookieService cookieService;

    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public JwtAuthenticationFilter(JwtService jwtService, AuthCookieService cookieService) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean userBound = false;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<AuthenticatedUser> user = this.cookieService.resolveToken(request)
                .flatMap(this.jwtService::parse);
            if (user.isPresent()) {
                authenticate(user.get(), request);
                userBound = true;
            }
        }
        try {
            chain.doFilter(request, response);
        }
        finally {
            if (userBound) {
                MDC.remove(MDC_USER_ID);
            }
        }
    }

    private void authenticate(AuthenticatedUser principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
            .authenticated(principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
        authentication.setDetails(this.detailsSource.buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        MDC.put(MDC_USER_ID, String.valueOf(principal.userId()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
