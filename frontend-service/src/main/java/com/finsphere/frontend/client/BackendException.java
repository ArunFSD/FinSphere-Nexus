package com.finsphere.frontend.client;

import java.io.Serial;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * A back-end service answered with an error. Carries the service's own message and per-field errors (from the
 * {@code ApiResponse} envelope) so forms can show them next to the fields they belong to.
 */
public class BackendException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int status;

    private final transient Map<String, String> fieldErrors;

    private final @Nullable String service;

    public BackendException(int status, @Nullable String message, Map<String, String> fieldErrors) {
        this(status, message, fieldErrors, null);
    }

    public BackendException(int status, @Nullable String message, Map<String, String> fieldErrors,
            @Nullable String service) {
        super((message == null || message.isBlank()) ? defaultMessage(status) : message);
        this.status = status;
        this.fieldErrors = Map.copyOf(fieldErrors);
        this.service = service;
    }

    public int getStatus() {
        return this.status;
    }

    /** Service id of the service that answered ({@code chit-finance-service}), when known. */
    public @Nullable String getService() {
        return this.service;
    }

    /** 503 or 504: the service is running but could not finish (its database or cache is down, or it timed out). */
    public boolean isServiceTrouble() {
        return this.status == 503 || this.status == 504;
    }

    public Map<String, String> getFieldErrors() {
        return this.fieldErrors;
    }

    public boolean is(HttpStatus expected) {
        return this.status == expected.value();
    }

    /**
     * The request was understood and refused, and the message is meant for the person who sent the form: any 4xx
     * other than 401 and 403, except a plain 404. A 404 with field errors is about a record named in the form (a
     * member the scheme service does not know yet, a month the scheme does not have), so it belongs next to that
     * field. A plain 404 means the page's own record is gone.
     */
    public boolean isRejection() {
        if (this.status == 404) {
            return !this.fieldErrors.isEmpty();
        }
        return this.status >= 400 && this.status < 500 && this.status != 401 && this.status != 403;
    }

    /** Message safe to show to people: the service's own text for rejections, a generic one for server faults. */
    public String userMessage() {
        return (this.status >= 500) ? defaultMessage(this.status) : getMessage();
    }

    private static String defaultMessage(int status) {
        if (status == 401) {
            return "Your session has ended. Sign in again.";
        }
        if (status == 403) {
            return "Your account does not have access to this.";
        }
        if (status == 404) {
            return "That record no longer exists.";
        }
        if (status == 429) {
            return "Too many attempts. Wait a little and try again.";
        }
        if (status >= 500) {
            return "The service could not complete this request. Try again in a minute.";
        }
        return "The request could not be completed.";
    }
}
