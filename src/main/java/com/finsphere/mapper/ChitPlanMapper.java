package com.finsphere.mapper;

import com.finsphere.common.model.chit.ChitPlanDTO;
import com.finsphere.entity.chit.ChitPlan;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ChitPlanMapper {

    ChitPlanDTO toDTO(ChitPlan entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "monthlyCycles", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    ChitPlan toEntity(ChitPlanDTO dto);
}