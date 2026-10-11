package com.finsphere.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

/**
 * JPA converter that stores a String column AES-GCM encrypted. Apply it per attribute:
 * {@code @Convert(converter = AesEncryptor.class)}. Requires {@code finsphere.security.encryption.key}.
 * Hibernate obtains it from the Spring container, so the {@link EncryptionService} is injected.
 */
@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    public AesEncryptor(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable String attribute) {
        return this.encryptionService.encrypt(attribute);
    }

    @Override
    public @Nullable String convertToEntityAttribute(@Nullable String dbData) {
        return this.encryptionService.decrypt(dbData);
    }
}
