package com.finsphere.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class CookieUtils {

    public void setHttpOnlyCookie(HttpServletResponse response, String token) {
        log.debug(">>>> [COOKIE_SET] Attaching fsn_auth_token to response");
        Cookie cookie = new Cookie("fsn_auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);
    }

    public void delHttpOnlyCookie(HttpServletResponse response) {
        log.info(">>>> [COOKIE_DELETE] Expiring fsn_auth_token cookie");
        Cookie cookie = new Cookie("fsn_auth_token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            log.warn(">>>> [COOKIE_EXTRACT_EMPTY] No cookies found in request");
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "fsn_auth_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseGet(() -> {
                    log.debug(">>>> [COOKIE_EXTRACT_MISSING] fsn_auth_token not found in cookies");
                    return null;
                });
    }
}