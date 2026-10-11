package com.finsphere.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Who is calling: client IP and user agent, already truncated to the sizes our audit tables accept.
 * <p>
 * The IP honours {@code X-Forwarded-For} because every service sits behind the API gateway (or the
 * frontend BFF), which sets that header. Do not expose the services directly to the internet.
 */
public record ClientInfo(String ipAddress, String userAgent) {

    public static final int MAX_IP_LENGTH = 64;

    public static final int MAX_USER_AGENT_LENGTH = 512;

    public static final String FORWARDED_FOR = "X-Forwarded-For";

    public static final ClientInfo UNKNOWN = new ClientInfo("unknown", "unknown");

    public static ClientInfo from(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR);
        String ip = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return new ClientInfo(truncate(ip, MAX_IP_LENGTH), truncate(userAgent, MAX_USER_AGENT_LENGTH));
    }

    /** Client info of the request bound to the current thread, or {@link #UNKNOWN} outside a request. */
    public static ClientInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return from(attributes.getRequest());
        }
        return UNKNOWN;
    }

    private static String truncate(@Nullable String value, int max) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
