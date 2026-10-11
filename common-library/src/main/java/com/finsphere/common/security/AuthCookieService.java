package com.finsphere.common.security;

import java.time.Duration;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * Reads and writes the HttpOnly session cookie that carries the JWT.
 */
public class AuthCookieService {

    private final CookieProperties properties;

    private final Duration maxAge;

    public AuthCookieService(CookieProperties properties, Duration maxAge) {
        this.properties = properties;
        this.maxAge = maxAge;
    }

    public String cookieName() {
        return this.properties.name();
    }

    public Optional<String> resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (this.properties.name().equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public void writeToken(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(token, this.maxAge).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO).toString());
    }

    private ResponseCookie build(String value, Duration age) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(this.properties.name(), value)
            .httpOnly(true)
            .secure(this.properties.secure())
            .sameSite(this.properties.sameSite())
            .path(this.properties.path())
            .maxAge(age);
        if (this.properties.domain() != null && !this.properties.domain().isBlank()) {
            builder.domain(this.properties.domain());
        }
        return builder.build();
    }
}
