package com.finsphere.chit.mapper;

import org.mapstruct.Mapper;

import com.finsphere.chit.domain.AuditTrailEntry;
import com.finsphere.common.audit.AuditRecord;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface AuditTrailMapper {

    AuditTrailEntry toEntry(AuditRecord record);
}
