package com.finsphere.common.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.finsphere.common.validation.ValidationGroups.FirstOrder;
import com.finsphere.common.validation.ValidationGroups.SecondOrder;
import com.finsphere.common.validation.ValidationGroups.ThirdOrder;

/**
 * Self-service registration. Validate with {@code ValidationGroups.Sequence}.
 * <p>
 * {@code ADMIN} registrations are accepted only from an authenticated administrator, or for the very first
 * administrator of a fresh installation. Everyone else registers as {@code CUSTOMER}.
 * <p>
 * Names may be written in any script (Tamil names keep their vowel signs, hence {@code \p{M}}). "Care of" also takes
 * the slash of the usual "S/o", "D/o", "W/o" and "C/o" prefixes.
 */
public record RegistrationRequest(
        @NotBlank(message = "Enter the full name", groups = FirstOrder.class)
        @Pattern(regexp = "^\\p{L}[\\p{L}\\p{M} .'-]*$",
                message = "Remove numbers and symbols from the name", groups = SecondOrder.class)
        @Size(max = 100, message = "Keep the name under 100 characters", groups = ThirdOrder.class)
        String fullName,

        @NotBlank(message = "Enter the mobile number", groups = FirstOrder.class)
        @Pattern(regexp = "^(\\+91[\\s-]?)?[6-9]\\d{9}$", message = "Enter a 10-digit mobile number",
                groups = SecondOrder.class)
        String phoneNumber,

        @Email(message = "Enter an e-mail address like name@example.com", groups = SecondOrder.class)
        @Size(max = 100, message = "Keep the e-mail address under 100 characters", groups = ThirdOrder.class)
        String email,

        @NotBlank(message = "Choose a password", groups = FirstOrder.class)
        @Size(min = 8, max = 30, message = "Use 8 to 30 characters for the password", groups = SecondOrder.class)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Use both letters and numbers in the password",
                groups = ThirdOrder.class)
        String password,

        @NotBlank(message = "Enter the address", groups = FirstOrder.class)
        @Size(max = 500, message = "Keep the address under 500 characters", groups = SecondOrder.class)
        String address,

        @Pattern(regexp = "^[\\p{L}\\p{M} .'/-]*$", message = "Use letters only here, for example S/o Mani",
                groups = SecondOrder.class)
        @Size(max = 100, message = "Keep this under 100 characters", groups = ThirdOrder.class)
        String careOf,

        @NotBlank(message = "Choose a role", groups = FirstOrder.class)
        @Pattern(regexp = "^(CUSTOMER|ADMIN)$", message = "Choose member or administrator",
                groups = SecondOrder.class)
        String role) {

    @Override
    public String toString() {
        return "RegistrationRequest[fullName=" + this.fullName + ", phoneNumber=" + this.phoneNumber + ", email="
                + this.email + ", role=" + this.role + ", password=****]";
    }
}
