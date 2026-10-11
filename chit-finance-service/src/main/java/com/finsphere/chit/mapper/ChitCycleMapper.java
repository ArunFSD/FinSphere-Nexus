package com.finsphere.chit.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.chit.domain.ChitMonthlyCycle;
import com.finsphere.common.dto.chit.ChitCycleResponse;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface ChitCycleMapper {

    @Mapping(target = "planId", source = "planId")
    @Mapping(target = "closed", source = "cycleClosed")
    ChitCycleResponse toResponse(ChitMonthlyCycle cycle);

    List<ChitCycleResponse> toResponses(List<ChitMonthlyCycle> cycles);
}
