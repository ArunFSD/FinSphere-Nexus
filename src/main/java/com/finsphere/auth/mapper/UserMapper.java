package com.finsphere.auth.mapper;

import com.finsphere.auth.dto.RegistrationRequest;
import com.finsphere.auth.entity.User;
import com.finsphere.auth.entity.CustomerProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "phoneNumber", source = "phoneNumber") // Added
    @Mapping(target = "email", source = "email")             // Added
    @Mapping(target = "profile", source = "request")
    User toEntity(RegistrationRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "careOf", source = "careOf")
    @Mapping(target = "address", source = "address")
    CustomerProfile toProfile(RegistrationRequest request);
}