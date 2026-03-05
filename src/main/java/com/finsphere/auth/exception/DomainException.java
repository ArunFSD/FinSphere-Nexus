package com.finsphere.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String topLevelMessage;

    public DomainException(HttpStatus status, String topLevelMessage, String detail) {
        super(detail);
        this.status = status;
        this.topLevelMessage = topLevelMessage;
    }
}
