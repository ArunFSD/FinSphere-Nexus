package com.finsphere.frontend.client;

import java.io.Serial;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

/** A back-end service could not be reached (no running instance, connection refused or timed out). */
public class BackendUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String service;

    public BackendUnavailableException(String service, Throwable cause) {
        super(service + " is not reachable: " + cause.getMessage(), cause);
        this.service = service;
    }

    public String getService() {
        return this.service;
    }

    /**
     * Whether the request may have reached the service and been carried out even though no answer came back: the
     * connection was made but the answer did not arrive within the read timeout. False when the request never got
     * there (no running instance, connection refused, connect timeout), so nothing can have been saved.
     */
    public boolean outcomeUnknown() {
        for (Throwable cause = getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException) {
                return false;
            }
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
