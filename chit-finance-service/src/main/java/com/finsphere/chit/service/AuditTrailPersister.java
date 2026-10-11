package com.finsphere.chit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.chit.mapper.AuditTrailMapper;
import com.finsphere.chit.repository.mongo.AuditTrailRepository;
import com.finsphere.common.audit.AuditRecord;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Stores every business audit record in MongoDB ({@code audit_trail}), asynchronously so audited operations are
 * never slowed down or failed by the audit store. The audit log file already holds the same record.
 */
@Component
public class AuditTrailPersister {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailPersister.class);

    private final AuditTrailRepository repository;

    private final AuditTrailMapper mapper;

    public AuditTrailPersister(AuditTrailRepository repository, AuditTrailMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Async
    @EventListener
    public void persist(AuditRecord record) {
        try {
            this.repository.save(this.mapper.toEntry(record));
        }
        catch (RuntimeException ex) {
            log.warn("Could not store audit record {} {}: {}", record.action(), record.resourceId(), ex.toString());
        }
    }
}
