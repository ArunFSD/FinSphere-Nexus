package com.finsphere.common.security;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AES-GCM field encryption settings. The {@link EncryptionService} is only created when a key is configured.
 *
 * @param key      16, 24 or 32 byte key (set {@code FSN_ENCRYPTION_KEY})
 * @param ivLength GCM nonce length in bytes, 12 is the NIST recommendation
 */
@ConfigurationProperties("finsphere.security.encryption")
public record EncryptionProperties(@Nullable String key, @DefaultValue("12") int ivLength) {
}
