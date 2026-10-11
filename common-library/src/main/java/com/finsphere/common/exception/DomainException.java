package com.finsphere.common.exception;

import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * A business rule violation that maps to a specific HTTP status and, optionally, to per-field messages
 * the UI can show next to the right input.
 */
public class DomainException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    private final Map<String, String> errors;

    public DomainException(HttpStatus status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = (errors == null || errors.isEmpty()) ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public DomainException(HttpStatus status, String message, String field, String fieldMessage) {
        this(status, message, Map.of(field, fieldMessage));
    }

    public DomainException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public static DomainException badRequest(String message, String field, String fieldMessage) {
        return new DomainException(HttpStatus.BAD_REQUEST, message, field, fieldMessage);
    }

    public static DomainException notFound(String message, String field, String fieldMessage) {
        return new DomainException(HttpStatus.NOT_FOUND, message, field, fieldMessage);
    }

    public static DomainException conflict(String message, String field, String fieldMessage) {
        return new DomainException(HttpStatus.CONFLICT, message, field, fieldMessage);
    }

    public static DomainException unauthorized(String message, String field, String fieldMessage) {
        return new DomainException(HttpStatus.UNAUTHORIZED, message, field, fieldMessage);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(HttpStatus.FORBIDDEN, message);
    }

    public HttpStatus getStatus() {
        return this.status;
    }

    public Map<String, String> getErrors() {
        return this.errors;
    }
}
