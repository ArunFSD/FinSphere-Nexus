package com.finsphere.chit.domain;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.audit.AuditOutcome;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Persisted business audit record (MongoDB collection {@code audit_trail}), queryable by time, actor and resource.
 */
@Document(collection = "audit_trail")
@CompoundIndex(name = "audit_resource", def = "{'resource': 1, 'resource_id': 1, 'timestamp': -1}")
@CompoundIndex(name = "audit_actor", def = "{'actor_id': 1, 'timestamp': -1}")
public class AuditTrailEntry {

    @Id
    private @Nullable String id;

    @Indexed
    @Field("timestamp")
    private Instant timestamp;

    @Field("service")
    private String service;

    @Field("action")
    private String action;

    @Field("resource")
    private String resource;

    @Field("resource_id")
    private @Nullable String resourceId;

    @Field("outcome")
    private AuditOutcome outcome;

    @Field("actor_id")
    private @Nullable Long actorId;

    @Field("actor_role")
    private @Nullable String actorRole;

    @Field("client_ip")
    private String clientIp;

    @Field("user_agent")
    private String userAgent;

    @Field("correlation_id")
    private @Nullable String correlationId;

    @Field("duration_ms")
    private long durationMs;

    @Field("detail")
    private @Nullable String detail;

    public @Nullable String getId() {
        return this.id;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return this.resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public @Nullable String getResourceId() {
        return this.resourceId;
    }

    public void setResourceId(@Nullable String resourceId) {
        this.resourceId = resourceId;
    }

    public AuditOutcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(AuditOutcome outcome) {
        this.outcome = outcome;
    }

    public @Nullable Long getActorId() {
        return this.actorId;
    }

    public void setActorId(@Nullable Long actorId) {
        this.actorId = actorId;
    }

    public @Nullable String getActorRole() {
        return this.actorRole;
    }

    public void setActorRole(@Nullable String actorRole) {
        this.actorRole = actorRole;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public @Nullable String getCorrelationId() {
        return this.correlationId;
    }

    public void setCorrelationId(@Nullable String correlationId) {
        this.correlationId = correlationId;
    }

    public long getDurationMs() {
        return this.durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public @Nullable String getDetail() {
        return this.detail;
    }

    public void setDetail(@Nullable String detail) {
        this.detail = detail;
    }
}
