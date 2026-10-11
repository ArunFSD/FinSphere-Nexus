package com.finsphere.frontend.client;

import jakarta.servlet.http.HttpServletRequest;

import com.finsphere.common.security.AuthCookieService;
import com.finsphere.common.web.ClientInfo;
import com.finsphere.common.web.CorrelationId;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Builds the {@link BackendCallContext} for the current thread. */
@Component
public class BackendCallContextResolver {

    private final AuthCookieService cookieService;

    public BackendCallContextResolver(AuthCookieService cookieService) {
        this.cookieService = cookieService;
    }

    public BackendCallContext current() {
        if (BackendCallContext.CURRENT.isBound()) {
            return BackendCallContext.CURRENT.get();
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return from(attributes.getRequest());
        }
        return BackendCallContext.NONE;
    }

    public BackendCallContext from(HttpServletRequest request) {
        String cookieHeader = this.cookieService.resolveToken(request)
            .map((token) -> this.cookieService.cookieName() + "=" + token)
            .orElse(null);
        String forwardedFor = request.getHeader(ClientInfo.FORWARDED_FOR);
        String chain = (forwardedFor == null || forwardedFor.isBlank()) ? request.getRemoteAddr()
                : forwardedFor + ", " + request.getRemoteAddr();
        return new BackendCallContext(cookieHeader, CorrelationId.current(), chain,
                request.getHeader(HttpHeaders.USER_AGENT));
    }
}
