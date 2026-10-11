package com.finsphere.auth.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TokenHasherTest {

    @Test
    void sha256IsStableLowerCaseHex() {
        // RFC 6234 test vector
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenHasher.sha256("abc"));
        assertEquals(64, TokenHasher.sha256("any.jwt.value").length());
        assertNotEquals(TokenHasher.sha256("a"), TokenHasher.sha256("b"));
    }
}
