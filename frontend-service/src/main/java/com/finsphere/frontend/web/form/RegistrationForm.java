package com.finsphere.frontend.web.form;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.dto.auth.RegistrationRequest;

/**
 * The sign-up and add-member forms. Adds the password confirmation the API does not need; {@code role} is only
 * honoured on the administrators' add-member form.
 */
public record RegistrationForm(@Nullable String fullName, @Nullable String phoneNumber, @Nullable String email,
        @Nullable String password, @Nullable String confirmPassword, @Nullable String address,
        @Nullable String careOf, @Nullable String role) {

    public static RegistrationForm empty(String role) {
        return new RegistrationForm("", "", "", "", "", "", "", role);
    }

    public boolean passwordsMatch() {
        return this.password != null && this.password.equals(this.confirmPassword);
    }

    public RegistrationRequest toRequest(String effectiveRole) {
        return new RegistrationRequest(trim(this.fullName), mobileNumber(this.phoneNumber), blankToNull(this.email),
                this.password, trim(this.address), blankToNull(this.careOf), effectiveRole);
    }

    /** People type "+91 98765 43210", "098765-43210" or "9876543210"; the API wants the ten digits. */
    static @Nullable String mobileNumber(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits.isEmpty() ? raw.trim() : digits;
    }

    /** The same form without the passwords, for showing it again after an error. */
    public RegistrationForm withoutPasswords() {
        return new RegistrationForm(this.fullName, this.phoneNumber, this.email, "", "", this.address, this.careOf,
                this.role);
    }

    private static @Nullable String trim(@Nullable String value) {
        return (value == null) ? null : value.trim();
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
