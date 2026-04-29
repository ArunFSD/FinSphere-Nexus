package com.finsphere.auth.mapper;


import com.finsphere.auth.entity.CustomerProfile;
import com.finsphere.auth.entity.User;
import com.finsphere.common.dto.auth.RegistrationRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "profile", source = "request")
    User toEntity(RegistrationRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "careOf", source = "careOf")
    @Mapping(target = "address", source = "address")
    CustomerProfile toProfile(RegistrationRequest request);
}