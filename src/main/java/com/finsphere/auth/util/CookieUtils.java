package com.finsphere.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieUtils {

    public void setHttpOnlyCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("fsn_auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);
    }

    public void delHttpOnlyCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("fsn_auth_token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // This tells the browser to delete it immediately
        response.addCookie(cookie);
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "fsn_auth_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

}
