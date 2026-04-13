package com.finsphere.auth.service;

import com.finsphere.auth.entity.LoginAudit;
import com.finsphere.auth.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final LoginAuditRepository auditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String phone, String email, String action, String ip, String ua, String remarks) {
        log.debug(">>>> [AUDIT_RECORD_START] Action: {} | UserID: {} | Phone: {}", action, userId, phone);

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
        log.info("<<<< [AUDIT_RECORD_SUCCESS] Action: {} saved for User: {}", action, phone);
    }
}