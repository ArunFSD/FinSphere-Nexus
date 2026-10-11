package com.finsphere.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finsphere.auth.domain.LoginAudit;
import com.finsphere.auth.repository.LoginAuditRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Persists login audit rows off the request thread. A login never waits for (or fails because of) the audit
 * insert; the same event is already in the audit log file, so a dropped row is never lost evidence.
 */
@Component
public class LoginAuditWriter {

    private static final Logger log = LoggerFactory.getLogger(LoginAuditWriter.class);

    private final LoginAuditRepository repository;

    public LoginAuditWriter(LoginAuditRepository repository) {
        this.repository = repository;
    }

    @Async
    public void save(LoginAudit entry) {
        try {
            this.repository.save(entry);
        }
        catch (RuntimeException ex) {
            log.warn("Could not persist login audit {} for user {}: {}", entry.getActionType(), entry.getUserId(),
                    ex.toString());
        }
    }
}
