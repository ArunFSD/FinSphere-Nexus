package com.finsphere.auth.service;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Canonical forms used for storage and lookup: phone numbers as their last 10 digits (drops +91, spaces and
 * dashes) and e-mail addresses trimmed and lower-cased.
 */
public final class IdentifierNormalizer {

    private IdentifierNormalizer() {
    }

    public static String phone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        return (digits.length() > 10) ? digits.substring(digits.length() - 10) : digits;
    }

    public static @Nullable String email(@Nullable String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** A login identifier is an e-mail when it contains {@code @}, otherwise a phone number. */
    public static String identifier(String raw) {
        String trimmed = raw.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase(Locale.ROOT) : phone(trimmed);
    }

    public static boolean isEmail(String identifier) {
        return identifier.contains("@");
    }
}
