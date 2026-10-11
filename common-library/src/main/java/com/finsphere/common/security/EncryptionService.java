package com.finsphere.common.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.Nullable;

/**
 * AES-GCM authenticated encryption for sensitive values (for example PII columns through {@link AesEncryptor}).
 * <p>
 * Output format: Base64( random IV || ciphertext+tag ). The key is validated once at startup and the
 * {@link SecureRandom} is shared, so encryption is cheap and thread-safe.
 */
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;

    private final int ivLength;

    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(EncryptionProperties properties) {
        String key = properties.key();
        if (key == null) {
            throw new IllegalStateException("finsphere.security.encryption.key is not configured");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "finsphere.security.encryption.key must be 16, 24 or 32 bytes long, found " + keyBytes.length);
        }
        if (properties.ivLength() < 12) {
            throw new IllegalStateException("finsphere.security.encryption.iv-length must be at least 12 bytes");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        this.ivLength = properties.ivLength();
    }

    public @Nullable String encrypt(@Nullable String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[this.ivLength];
            this.secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, this.keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder()
                .encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES encryption failed", ex);
        }
    }

    public @Nullable String decrypt(@Nullable String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(cipherText));
            byte[] iv = new byte[this.ivLength];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, this.keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException | java.nio.BufferUnderflowException ex) {
            throw new IllegalStateException("AES decryption failed, data is corrupted or the key changed", ex);
        }
    }
}
