package com.finsphere.common.exception;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.common.api.ApiResponse;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * Builds error responses and logs each failure once, at a level that matches its severity.
 */
final class ErrorResponder {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponder() {
    }

    static ResponseEntity<ApiResponse<Void>> respond(HttpStatusCode status, String message,
            Map<String, String> errors, HttpServletRequest request, Exception ex) {
        String path = request.getRequestURI();
        if (status.is5xxServerError()) {
            log.error("{} {} failed with {}", request.getMethod(), path, status.value(), ex);
        }
        else if (status.value() == 401 || status.value() == 403 || status.value() == 404) {
            log.info("{} {} -> {} {}", request.getMethod(), path, status.value(), message);
        }
        else {
            log.warn("{} {} -> {} {} {}", request.getMethod(), path, status.value(), message,
                    errors.isEmpty() ? "" : errors.keySet());
        }
        return ResponseEntity.status(status).body(ApiResponse.failure(status, message, errors, path));
    }
}
