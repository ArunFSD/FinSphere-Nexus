package com.finsphere.chit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.chit.domain.ChitPlan;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface ChitPlanMapper {

    @Mapping(target = "paymentWindowDays", source = "paymentDuration")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "cycles", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    ChitPlan toEntity(ChitPlanRequest request);

    /**
     * @param plan          the scheme
     * @param filledSlots   active members, computed separately (one grouped query per page instead of N+1)
     * @param settledMonths months already auctioned, computed the same way
     */
    @Mapping(target = "filledSlots", source = "filledSlots")
    @Mapping(target = "settledMonths", source = "settledMonths")
    ChitPlanResponse toResponse(ChitPlan plan, long filledSlots, long settledMonths);
}
