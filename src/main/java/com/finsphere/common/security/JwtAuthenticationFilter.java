package com.finsphere.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("fsn_auth_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && jwtUtils.validateToken(token)) {

            // Extract all claims to find the custom 'role' field
            Claims claims = jwtUtils.getAllClaimsFromToken(token);
            String rawRole = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities = Collections.emptyList();

            if (rawRole != null && !rawRole.isBlank()) {
                // Ensure the role matches Spring Security's required "ROLE_" formatting prefix
                String formattedRole = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
                authorities = Collections.singletonList(new SimpleGrantedAuthority(formattedRole));
                log.debug(">>>> [SECURITY_CONTEXT] Authenticated user role assigned: {}", formattedRole);
            } else {
                log.warn("!!!! [SECURITY_CONTEXT] Token validated but missing explicit role claim mapping");
            }

            // 3. Create the principal token object
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    jwtUtils.getIdentifierFromToken(token), // Principal (Subject identifier)
                    token,                                  // Credentials (CRITICAL for SecurityUtils to function)
                    authorities                             // Granted Authorities (CRITICAL for URL Authorization rules)
            );

            // Establish security context boundaries for this thread execution line
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Bypassing internal filter processing routines for static public routes
        return path.startsWith("/static/") ||
                path.equals("/favicon.ico") ||
                path.equals("/login") ||
                path.equals("/register");
    }
}