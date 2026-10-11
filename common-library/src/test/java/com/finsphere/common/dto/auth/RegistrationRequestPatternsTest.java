package com.finsphere.common.dto.auth;

import jakarta.validation.constraints.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sign-up patterns, checked with the exact expressions on the record so the test follows any change to them.
 */
class RegistrationRequestPatternsTest {

    @Test
    void namesMayUseAnyScriptButNoDigitsOrSymbols() {
        assertTrue(matches("fullName", "Arun S"));
        assertTrue(matches("fullName", "K. Lakshmi"));
        assertTrue(matches("fullName", "D'Souza Mary-Ann"));
        assertTrue(matches("fullName", "முருகன் செல்வம்"));
        assertFalse(matches("fullName", "Arun 2"));
        assertFalse(matches("fullName", " Arun"));
        assertFalse(matches("fullName", "<b>Arun</b>"));
    }

    @Test
    void careOfTakesTheUsualRelationPrefixes() {
        assertTrue(matches("careOf", "S/o Mani"));
        assertTrue(matches("careOf", "W/o R. Kumar"));
        assertTrue(matches("careOf", "த/பெ முருகன்"));
        assertTrue(matches("careOf", ""));
        assertFalse(matches("careOf", "S/o 42"));
        assertFalse(matches("careOf", "Mani; DROP"));
    }

    @Test
    void mobileNumbersAreIndianTenDigitNumbers() {
        assertTrue(matches("phoneNumber", "9876543210"));
        assertTrue(matches("phoneNumber", "+91 9876543210"));
        assertTrue(matches("phoneNumber", "+91-6123456789"));
        assertFalse(matches("phoneNumber", "5876543210"));
        assertFalse(matches("phoneNumber", "987654321"));
    }

    @Test
    void passwordsNeedLettersAndNumbers() {
        assertTrue(matches("password", "Member1234"));
        assertFalse(matches("password", "onlyletters"));
        assertFalse(matches("password", "1234567890"));
    }

    private static boolean matches(String component, String value) {
        try {
            Pattern pattern = RegistrationRequest.class.getDeclaredField(component).getAnnotation(Pattern.class);
            return java.util.regex.Pattern.matches(pattern.regexp(), value);
        }
        catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(component, ex);
        }
    }
}
