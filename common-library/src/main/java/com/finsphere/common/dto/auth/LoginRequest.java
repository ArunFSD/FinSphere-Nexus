package com.finsphere.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login with a registered phone number or e-mail address.
 *
 * @param identifier phone number (10 digits, optional +91 prefix) or e-mail
 * @param password   plain-text password (never logged)
 */
public record LoginRequest(
        @NotBlank(message = "Enter your mobile number or e-mail")
        @Size(max = 100, message = "That is longer than any mobile number or e-mail we hold")
        String identifier,

        @NotBlank(message = "Enter your password")
        @Size(max = 128, message = "That password is too long")
        String password) {

    /** Never print the password, records otherwise include every component in toString(). */
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + this.identifier + ", password=****]";
    }
}
