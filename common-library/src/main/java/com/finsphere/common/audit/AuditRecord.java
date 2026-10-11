package com.finsphere.common.audit;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * One entry of the business audit trail: who did what, to which record, from where, and how it ended.
 * <p>
 * Every record is written as a JSON line to {@code <app>-audit.log} and published as a Spring application event,
 * so a service can also persist it (chit-finance stores them in MongoDB, auth-service in {@code login_audit}).
 *
 * @param timestamp     when the action completed
 * @param service       service name
 * @param action        what happened, e.g. {@code CHIT_PLAN_CREATED}
 * @param resource      what kind of record, e.g. {@code CHIT_PLAN}
 * @param resourceId    which record, when known
 * @param outcome       success or failure
 * @param actorId       user id of the caller, when signed in
 * @param actorRole     role of the caller, when signed in
 * @param clientIp      caller IP
 * @param userAgent     caller user agent
 * @param correlationId request correlation id
 * @param durationMs    execution time
 * @param detail        failure reason or extra context
 */
public record AuditRecord(
        Instant timestamp,
        String service,
        String action,
        String resource,
        @Nullable String resourceId,
        AuditOutcome outcome,
        @Nullable Long actorId,
        @Nullable String actorRole,
        String clientIp,
        String userAgent,
        @Nullable String correlationId,
        long durationMs,
        @Nullable String detail) {
}
