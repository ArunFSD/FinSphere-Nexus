package com.finsphere.common.audit;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.ApplicationEventPublisher;

/**
 * Entry point of the business audit trail.
 * <p>
 * {@link #record(AuditRecord)} writes the record as one JSON line to the {@code AUDIT} logger (routed to
 * {@code <app>-audit.log} by the shared Logback config) and then publishes it as an application event for
 * optional persistence. Recording never throws: an audit problem must not fail the business operation.
 */
public class AuditTrail {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    private static final Logger log = LoggerFactory.getLogger(AuditTrail.class);

    private final JsonMapper jsonMapper;

    private final ApplicationEventPublisher publisher;

    private final String serviceName;

    private final Clock clock;

    public AuditTrail(JsonMapper jsonMapper, ApplicationEventPublisher publisher, String serviceName, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.publisher = publisher;
        this.serviceName = serviceName;
        this.clock = clock;
    }

    /** Starts a record for {@code action} on {@code resource}; finish with {@link AuditEventBuilder#record()}. */
    public AuditEventBuilder event(String action, String resource) {
        return new AuditEventBuilder(this, this.serviceName, this.clock, action, resource);
    }

    public void record(AuditRecord record) {
        try {
            AUDIT_LOG.info(this.jsonMapper.writeValueAsString(record));
        }
        catch (JacksonException ex) {
            log.warn("Could not serialise audit record {}", record.action(), ex);
            AUDIT_LOG.info(record.toString());
        }
        try {
            this.publisher.publishEvent(record);
        }
        catch (RuntimeException ex) {
            log.warn("Audit listener failed for {} {}", record.action(), record.resourceId(), ex);
        }
    }
}
