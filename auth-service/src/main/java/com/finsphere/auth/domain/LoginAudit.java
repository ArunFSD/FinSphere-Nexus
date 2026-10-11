package com.finsphere.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Append-only security event (registration, login, logout). Table {@code fsn_identity.login_audit}.
 */
@Entity
@Table(name = "login_audit")
public class LoginAudit {

    public static final int MAX_REMARKS = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private @Nullable Long userId;

    @Column(name = "phone_number")
    private @Nullable String phoneNumber;

    @Column(name = "email")
    private @Nullable String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private LoginAuditAction actionType;

    @Column(name = "ip_address", length = 64)
    private @Nullable String ipAddress;

    @Column(name = "user_agent", length = 512)
    private @Nullable String userAgent;

    @Column(name = "remarks", length = MAX_REMARKS)
    private @Nullable String remarks;

    @Column(name = "correlation_id", length = 64)
    private @Nullable String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LoginAudit() {
        // for JPA
    }

    public LoginAudit(LoginAuditAction actionType, @Nullable Long userId, @Nullable String phoneNumber,
            @Nullable String email, @Nullable String ipAddress, @Nullable String userAgent, @Nullable String remarks,
            @Nullable String correlationId, Instant createdAt) {
        this.actionType = actionType;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.remarks = (remarks != null && remarks.length() > MAX_REMARKS) ? remarks.substring(0, MAX_REMARKS)
                : remarks;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return this.id;
    }

    public @Nullable Long getUserId() {
        return this.userId;
    }

    public @Nullable String getPhoneNumber() {
        return this.phoneNumber;
    }

    public @Nullable String getEmail() {
        return this.email;
    }

    public LoginAuditAction getActionType() {
        return this.actionType;
    }

    public @Nullable String getIpAddress() {
        return this.ipAddress;
    }

    public @Nullable String getUserAgent() {
        return this.userAgent;
    }

    public @Nullable String getRemarks() {
        return this.remarks;
    }

    public @Nullable String getCorrelationId() {
        return this.correlationId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
