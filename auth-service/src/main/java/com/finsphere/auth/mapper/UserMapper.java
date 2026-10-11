package com.finsphere.auth.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.finsphere.auth.domain.CustomerProfile;
import com.finsphere.auth.domain.UserAccount;
import com.finsphere.common.dto.auth.LoginResponse;
import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.auth.UserProfileResponse;
import com.finsphere.common.event.UserUpdateEvent;
import com.finsphere.common.mapping.CentralMapperConfig;

/**
 * Maps between registration requests, identity entities, API responses and Kafka events.
 * Unmapped target properties fail the build (see {@link CentralMapperConfig}).
 */
@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    /** Phone, e-mail, password and role need normalisation or hashing and are set by the service. */
    CustomerProfile toProfile(RegistrationRequest request);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "fullName", source = "profile.fullName")
    @Mapping(target = "careOf", source = "profile.careOf")
    @Mapping(target = "address", source = "profile.address")
    @Mapping(target = "memberSince", source = "createdAt")
    UserProfileResponse toProfileResponse(UserAccount user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "fullName", expression = "java(user.displayName())")
    LoginResponse toLoginResponse(UserAccount user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", expression = "java(user.displayName())")
    @Mapping(target = "careOf", source = "user.profile.careOf")
    UserUpdateEvent toUpdateEvent(UserAccount user, Instant occurredAt);
}
