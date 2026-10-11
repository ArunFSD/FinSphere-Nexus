package com.finsphere.common.audit;

import java.util.Optional;

import com.finsphere.common.security.SecurityUtils;

import org.springframework.data.domain.AuditorAware;

/**
 * Supplies the signed-in user id to Spring Data auditing ({@code @CreatedBy} / {@code @LastModifiedBy}).
 */
public class SecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return SecurityUtils.currentUserId();
    }
}
