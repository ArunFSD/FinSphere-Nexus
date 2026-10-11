package com.finsphere.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierNormalizerTest {

    @Test
    void phoneNumbersAreReducedToTheTenDigitNationalNumber() {
        assertEquals("9876543210", IdentifierNormalizer.phone("9876543210"));
        assertEquals("9876543210", IdentifierNormalizer.phone("+91 98765-43210"));
        assertEquals("9876543210", IdentifierNormalizer.phone("09876543210"));
    }

    @Test
    void emailsAreTrimmedAndLowerCasedAndBlankMeansNone() {
        assertEquals("ravi@example.com", IdentifierNormalizer.email("  Ravi@Example.COM "));
        assertNull(IdentifierNormalizer.email("   "));
        assertNull(IdentifierNormalizer.email(null));
    }

    @Test
    void loginIdentifierIsEitherAnEmailOrAPhoneNumber() {
        assertEquals("ravi@example.com", IdentifierNormalizer.identifier(" Ravi@Example.com"));
        assertEquals("9876543210", IdentifierNormalizer.identifier("+91 9876543210"));
        assertTrue(IdentifierNormalizer.isEmail("ravi@example.com"));
        assertFalse(IdentifierNormalizer.isEmail("9876543210"));
    }
}
