package com.finsphere.common.audit;

import java.time.Clock;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.common.web.ClientInfo;
import com.finsphere.common.web.CorrelationId;

/**
 * Fluent builder for an {@link AuditRecord}. Actor, client and correlation id default to the current request.
 */
public final class AuditEventBuilder {

    private static final int MAX_DETAIL_LENGTH = 500;

    private final AuditTrail trail;

    private final String service;

    private final Clock clock;

    private final String action;

    private final String resource;

    private @Nullable String resourceId;

    private AuditOutcome outcome = AuditOutcome.SUCCESS;

    private @Nullable String detail;

    private long durationMs;

    private @Nullable Long actorId;

    private @Nullable String actorRole;

    private boolean actorSet;

    private @Nullable ClientInfo client;

    AuditEventBuilder(AuditTrail trail, String service, Clock clock, String action, String resource) {
        this.trail = trail;
        this.service = service;
        this.clock = clock;
        this.action = action;
        this.resource = resource;
    }

    public AuditEventBuilder resourceId(@Nullable Object id) {
        this.resourceId = (id != null) ? String.valueOf(id) : null;
        return this;
    }

    public AuditEventBuilder outcome(AuditOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public AuditEventBuilder failure(@Nullable String reason) {
        this.outcome = AuditOutcome.FAILURE;
        return detail(reason);
    }

    public AuditEventBuilder detail(@Nullable String detail) {
        this.detail = (detail != null && detail.length() > MAX_DETAIL_LENGTH) ? detail.substring(0, MAX_DETAIL_LENGTH)
                : detail;
        return this;
    }

    public AuditEventBuilder durationMs(long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    /** Overrides the actor, for actions performed before a security context exists (login, registration). */
    public AuditEventBuilder actor(@Nullable Long userId, @Nullable String role) {
        this.actorId = userId;
        this.actorRole = role;
        this.actorSet = true;
        return this;
    }

    public AuditEventBuilder client(ClientInfo client) {
        this.client = client;
        return this;
    }

    public AuditRecord build() {
        Long actor = this.actorId;
        String role = this.actorRole;
        if (!this.actorSet) {
            AuthenticatedUser user = SecurityUtils.currentUser().orElse(null);
            actor = (user != null) ? user.userId() : null;
            role = (user != null) ? user.role() : null;
        }
        ClientInfo caller = Objects.requireNonNullElseGet(this.client, ClientInfo::current);
        return new AuditRecord(this.clock.instant(), this.service, this.action, this.resource, this.resourceId,
                this.outcome, actor, role, caller.ipAddress(), caller.userAgent(), CorrelationId.current(),
                this.durationMs, this.detail);
    }

    public void record() {
        this.trail.record(build());
    }
}
