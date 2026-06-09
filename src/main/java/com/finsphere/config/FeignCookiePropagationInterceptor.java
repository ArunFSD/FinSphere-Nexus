package com.finsphere.config;

import feign.RequestInterceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignCookiePropagationInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1. Grab the original incoming request details from the current thread execution context
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest originalRequest = attributes.getRequest();

            // 2. Extract the Cookie header which contains your authentication token string
            String cookieHeader = originalRequest.getHeader(HttpHeaders.COOKIE);

            // 3. Propagate the exact cookie down the pipeline through the API Gateway
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                template.header(HttpHeaders.COOKIE, cookieHeader);
            }
        }
    }
}