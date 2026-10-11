package com.finsphere.common.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds a correlation id to every request: reuses the inbound {@code X-Correlation-Id} header when it is
 * well-formed, otherwise creates one, exposes it in the MDC for logging and echoes it on the response.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // On an error dispatch (/error forward) keep the id that was assigned to the original request
        String correlationId = (request.getAttribute(REQUEST_ATTRIBUTE) instanceof String existing) ? existing
                : CorrelationId.sanitizeOrCreate(request.getHeader(CorrelationId.HEADER));
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        }
        finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
