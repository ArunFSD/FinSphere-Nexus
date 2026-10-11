package com.finsphere.auth.repository;

import com.finsphere.auth.domain.LoginAudit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

}
