package com.finsphere.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class EncryptionUtils {

    @Value("${finsphere.security.encryption-key}")
    private String secretKey;

    @Value("${finsphere.security.iv-length:12}")
    private int ivLength;

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int TAG_BIT_LEN = 128;

    public String encrypt(String data) {
        if (data == null || data.isEmpty()) return null;

        if (secretKey == null || secretKey.length() != 32) {
            log.error("!!!! [CONFIG_ERROR] AES key must be 32 chars. Found: {}",
                    (secretKey == null ? "null" : secretKey.length()));
            throw new RuntimeException("Encryption key configuration mismatch");
        }

        try {
            byte[] iv = new byte[ivLength];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LEN, iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
            bb.put(iv);
            bb.put(encrypted);

            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            log.error("!!!! [CRYPTO_ERROR] Encryption failed: {}", e.getMessage());
            throw new RuntimeException("AES Encryption failed for FinSphere", e);
        }
    }

    public String decrypt(String encryptedDataBase64) {
        if (encryptedDataBase64 == null || encryptedDataBase64.isEmpty()) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedDataBase64);
            ByteBuffer bb = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[ivLength];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LEN, iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("!!!! [CRYPTO_ERROR] Decryption failed: {}", e.getMessage());
            throw new RuntimeException("AES Decryption failed - Data may be corrupted", e);
        }
    }
}