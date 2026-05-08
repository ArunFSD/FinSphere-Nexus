package com.finsphere.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
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
                if ("fsn_auth_token".equals(cookie.getName())) { // Match your cookie name
                    token = cookie.getValue();
                }
            }
        }

        if (token != null && jwtUtils.validateToken(token)) {
            // This is the CRITICAL part: putting the token into the "Credentials"
            // so your SecurityUtils.getClaims() can find it.
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    jwtUtils.getIdentifierFromToken(token),
                    token, // Store token here for SecurityUtils
                    Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
