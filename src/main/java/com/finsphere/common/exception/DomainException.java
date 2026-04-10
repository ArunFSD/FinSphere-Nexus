package com.finsphere.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String topLevelMessage;
    private final Map<String, String> errors; // Changed from String detail

    public DomainException(HttpStatus status, String topLevelMessage, Map<String, String> errors) {
        super(topLevelMessage); // Super message is usually the general title
        this.status = status;
        this.topLevelMessage = topLevelMessage;
        this.errors = errors;
    }

    // Helper constructor for when you only have ONE error
    public DomainException(HttpStatus status, String topLevelMessage, String field, String message) {
        super(topLevelMessage);
        this.status = status;
        this.topLevelMessage = topLevelMessage;
        this.errors = Map.of(field, message);
    }
}
