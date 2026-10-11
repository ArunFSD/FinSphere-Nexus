package com.finsphere.chit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.finsphere.chit.domain.UserMirror;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface UserMirrorMapper {

    /** {@code lastUpdatedAt} is set by the caller, which also decides whether the event is stale. */
    @Mapping(target = "lastUpdatedAt", ignore = true)
    UserMirror toEntity(UserUpdateEvent event);

    /** Applies an update to an existing mirror row; the id is never changed. */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    void update(UserUpdateEvent event, @MappingTarget UserMirror mirror);

    MemberResponse toMember(UserMirror mirror);
}
