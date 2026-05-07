package com.finsphere.common.security;

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
public class EncryptionUtils {

    @Value("${finsphere.security.encryption-key}")
    private String secretKey;

    @Value("${finsphere.security.iv-length}")
    private int ivLength;

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int TAG_BIT_LEN = 128;

    public String encrypt(String data) {
        if (data == null) return null;
        try {
            // Use the ivLength from your properties
            byte[] iv = new byte[ivLength];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(TAG_BIT_LEN, iv));

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Industrial Standard: Store IV and Encrypted data together
            ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
            bb.put(iv);
            bb.put(encrypted);

            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException("AES Encryption failed for FinSphere", e);
        }
    }

    public String decrypt(String encryptedDataBase64) {
        if (encryptedDataBase64 == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedDataBase64);
            ByteBuffer bb = ByteBuffer.wrap(decoded);

            // Extract the IV based on the ivLength from properties
            byte[] iv = new byte[ivLength];
            bb.get(iv);

            byte[] cipherText = new byte[bb.remaining()];
            bb.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(TAG_BIT_LEN, iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES Decryption failed - Data may be corrupted", e);
        }
    }
}
