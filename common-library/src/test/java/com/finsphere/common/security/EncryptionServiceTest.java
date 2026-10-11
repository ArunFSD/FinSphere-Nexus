package com.finsphere.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionServiceTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef";

    private final EncryptionService service = new EncryptionService(new EncryptionProperties(KEY, 12));

    @Test
    void roundTrip() {
        String secret = "Aadhaar 1234 5678 9012 / ₹ ಕನ್ನಡ";
        assertEquals(secret, this.service.decrypt(this.service.encrypt(secret)));
    }

    @Test
    void everyEncryptionUsesAFreshNonce() {
        assertNotEquals(this.service.encrypt("same"), this.service.encrypt("same"));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertNull(this.service.encrypt(null));
        assertNull(this.service.decrypt(null));
        assertEquals("", this.service.encrypt(""));
    }

    @Test
    void tamperedOrForeignCipherTextIsRejected() {
        String cipherText = this.service.encrypt("value");
        EncryptionService otherKey = new EncryptionService(new EncryptionProperties("fedcba9876543210", 12));
        assertThrows(IllegalStateException.class, () -> otherKey.decrypt(cipherText));
        assertThrows(IllegalStateException.class, () -> this.service.decrypt("not-base64!"));
        assertThrows(IllegalStateException.class, () -> this.service.decrypt("AAAA"));
    }

    @Test
    void invalidConfigurationFailsFast() {
        assertThrows(IllegalStateException.class, () -> new EncryptionService(new EncryptionProperties(null, 12)));
        assertThrows(IllegalStateException.class, () -> new EncryptionService(new EncryptionProperties("short", 12)));
        assertThrows(IllegalStateException.class, () -> new EncryptionService(new EncryptionProperties(KEY, 8)));
    }
}
