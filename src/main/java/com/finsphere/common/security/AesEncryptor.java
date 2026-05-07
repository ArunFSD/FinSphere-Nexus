package com.finsphere.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter
@RequiredArgsConstructor
public class AesEncryptor implements AttributeConverter<String, String> {

    private final EncryptionUtils encryptionUtils;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute != null ? encryptionUtils.encrypt(attribute) : null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData != null ? encryptionUtils.decrypt(dbData) : null;
    }
}
