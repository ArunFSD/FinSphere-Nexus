package com.finsphere.frontend.client;

import java.io.IOException;

import com.finsphere.common.web.ClientInfo;
import com.finsphere.common.web.CorrelationId;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Adds the caller's session cookie, correlation id, client address and user agent to every back-end call, and turns
 * "nothing to call" failures (no registered instance, connection refused, timeout) into
 * {@link BackendUnavailableException} so pages can say which part of the system is down.
 */
public class BackendRequestInterceptor implements ClientHttpRequestInterceptor {

    private final String service;

    private final BackendCallContextResolver contextResolver;

    public BackendRequestInterceptor(String service, BackendCallContextResolver contextResolver) {
        this.service = service;
        this.contextResolver = contextResolver;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        BackendCallContext context = this.contextResolver.current();
        HttpHeaders headers = request.getHeaders();
        if (context.cookieHeader() != null) {
            headers.set(HttpHeaders.COOKIE, context.cookieHeader());
        }
        if (context.correlationId() != null) {
            headers.set(CorrelationId.HEADER, context.correlationId());
        }
        if (context.forwardedFor() != null) {
            headers.set(ClientInfo.FORWARDED_FOR, context.forwardedFor());
        }
        if (context.userAgent() != null) {
            headers.set(HttpHeaders.USER_AGENT, context.userAgent());
        }
        try {
            return execution.execute(request, body);
        }
        catch (IOException ex) {
            throw new BackendUnavailableException(this.service, ex);
        }
        catch (IllegalStateException ex) {
            // Spring Cloud LoadBalancer: "No instances available for <service>"
            throw new BackendUnavailableException(this.service, ex);
        }
    }
}
