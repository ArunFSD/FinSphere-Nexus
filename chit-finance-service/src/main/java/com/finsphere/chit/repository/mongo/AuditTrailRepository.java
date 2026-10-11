package com.finsphere.chit.repository.mongo;

import com.finsphere.chit.domain.AuditTrailEntry;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditTrailRepository extends MongoRepository<AuditTrailEntry, String> {

}
