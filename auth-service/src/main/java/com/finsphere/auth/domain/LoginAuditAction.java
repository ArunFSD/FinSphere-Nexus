package com.finsphere.auth.domain;

/**
 * Security events recorded in {@code login_audit}.
 */
public enum LoginAuditAction {

    REGISTER_SUCCESS(false),

    REGISTER_FAILURE(true),

    LOGIN_SUCCESS(false),

    LOGIN_FAILURE(true),

    /** Rejected because the identifier is temporarily locked after repeated failures. */
    LOGIN_BLOCKED(true),

    LOGOUT(false),

    /** Older sessions were signed out because the user signed in again elsewhere. */
    SESSION_REPLACED(false);

    private final boolean failure;

    LoginAuditAction(boolean failure) {
        this.failure = failure;
    }

    public boolean isFailure() {
        return this.failure;
    }
}
