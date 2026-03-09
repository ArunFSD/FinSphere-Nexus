package com.finsphere.auth.service;

import com.finsphere.auth.entity.LoginAudit;
import com.finsphere.auth.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final LoginAuditRepository auditRepository;

    /**
     * Propagation.REQUIRES_NEW ensures the audit is saved even if the
     * main login transaction fails (e.g., during a wrong password attempt).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String phone, String email, String action, String ip, String ua, String remarks) {
        LoginAudit audit = LoginAudit.builder()
                .userId(userId)
                .phoneNumber(phone)
                .email(email)
                .actionType(action)
                .ipAddress(ip)
                .userAgent(ua)
                .remarks(remarks)
                .build();
        auditRepository.save(audit);
    }
}
