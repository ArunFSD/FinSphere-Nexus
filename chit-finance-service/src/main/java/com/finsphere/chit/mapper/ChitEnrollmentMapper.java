package com.finsphere.chit.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.chit.domain.ChitEnrollment;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface ChitEnrollmentMapper {

    /**
     * @param enrollment the enrollment
     * @param memberName member name from the user directory mirror, {@code null} when not known yet
     */
    @Mapping(target = "id", source = "enrollment.id")
    @Mapping(target = "planId", source = "enrollment.plan.id")
    @Mapping(target = "planName", source = "enrollment.plan.name")
    @Mapping(target = "memberName", source = "memberName")
    ChitEnrollmentResponse toResponse(ChitEnrollment enrollment, @Nullable String memberName);
}
