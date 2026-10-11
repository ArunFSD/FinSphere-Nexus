package com.finsphere.common.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import com.finsphere.common.web.CorrelationId;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * The single response envelope used by every FinSphere REST endpoint, for success and failure alike.
 *
 * @param success       {@code true} for 2xx responses
 * @param status        HTTP status code, repeated in the body for convenience
 * @param message       human readable summary
 * @param data          payload on success
 * @param errors        field name to error message, on validation or business failures
 * @param path          request path, on failures
 * @param correlationId id to quote when reporting a problem (matches the server logs)
 * @param timestamp     when the response was produced (UTC)
 * @param <T>           payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        @Nullable String message,
        @Nullable T data,
        @Nullable Map<String, String> errors,
        @Nullable String path,
        @Nullable String correlationId,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(HttpStatusCode status, String message, @Nullable T data) {
        return new ApiResponse<>(true, status.value(), message, data, null, null, CorrelationId.current(),
                Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, @Nullable T data) {
        return success(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(String message, @Nullable T data) {
        return success(HttpStatus.CREATED, message, data);
    }

    public static <T> ApiResponse<T> failure(HttpStatusCode status, String message,
            @Nullable Map<String, String> errors, @Nullable String path) {
        Map<String, String> safeErrors = (errors == null || errors.isEmpty()) ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
        return new ApiResponse<>(false, status.value(), message, null, safeErrors, path, CorrelationId.current(),
                Instant.now());
    }

    /** Wraps this envelope in a {@link ResponseEntity} carrying the same status code. */
    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(this.status).body(this);
    }
}
