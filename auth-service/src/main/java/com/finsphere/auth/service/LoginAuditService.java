package com.finsphere.auth.service;

import java.time.Clock;

import org.jspecify.annotations.Nullable;

import com.finsphere.auth.domain.LoginAudit;
import com.finsphere.auth.domain.LoginAuditAction;
import com.finsphere.common.audit.AuditOutcome;
import com.finsphere.common.audit.AuditTrail;
import com.finsphere.common.web.ClientInfo;
import com.finsphere.common.web.CorrelationId;

import org.springframework.stereotype.Service;

/**
 * Records security events twice: synchronously in the audit log file (via {@link AuditTrail}) and
 * asynchronously in the {@code login_audit} table.
 */
@Service
public class LoginAuditService {

    private static final String RESOURCE = "USER_ACCOUNT";

    private final AuditTrail auditTrail;

    private final LoginAuditWriter writer;

    private final Clock clock;

    public LoginAuditService(AuditTrail auditTrail, LoginAuditWriter writer, Clock clock) {
        this.auditTrail = auditTrail;
        this.writer = writer;
        this.clock = clock;
    }

    public void record(LoginAuditAction action, @Nullable Long userId, @Nullable String phoneNumber,
            @Nullable String email, ClientInfo client, @Nullable String remarks) {
        this.auditTrail.event(action.name(), RESOURCE)
            .resourceId(userId)
            .actor(userId, null)
            .client(client)
            .outcome(action.isFailure() ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS)
            .detail(remarks)
            .record();
        this.writer.save(new LoginAudit(action, userId, phoneNumber, email, client.ipAddress(), client.userAgent(),
                remarks, CorrelationId.current(), this.clock.instant()));
    }
}
