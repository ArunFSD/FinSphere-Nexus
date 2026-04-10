package com.finsphere.auth.dto;

import com.finsphere.auth.validation.ValidationGroups.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotBlank(
            message = "Full name is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^[a-zA-Z\\s]+$",
            message = "Full name can only contain letters and spaces",
            groups = SecondOrder.class
    )
    @Size(
            max = 100,
            message = "Full name must not exceed 100 characters",
            groups = ThirdOrder.class
    )
    private String fullName;

    @NotBlank(
            message = "Phone number is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid phone number",
            groups = SecondOrder.class
    )
    private String phoneNumber;

    @Email(
            message = "Invalid email format",
            groups = SecondOrder.class
    )
    @Size(
            max = 100,
            message = "Email must not exceed 100 characters",
            groups = ThirdOrder.class
    )
    private String email;

    @NotBlank(
            message = "Password is mandatory",
            groups = FirstOrder.class
    )
    @Size(
            min = 8,
            message = "Password must be at least 8 characters",
            groups = SecondOrder.class
    )
    @Size(
            max = 30,
            message = "Password must not exceed 30 characters",
            groups = ThirdOrder.class
    )
    private String password;

    @NotBlank(
            message = "Address is mandatory",
            groups = FirstOrder.class
    )
    @Size(
            max = 500,
            message = "Address must not exceed 500 characters",
            groups = SecondOrder.class
    )
    private String address;

    @Pattern(
            regexp = "^[a-zA-Z\\s]*$",
            message = "Care of (C/O) can only contain letters and spaces",
            groups = SecondOrder.class
    )
    @Size(
            max = 100,
            message = "Care of (C/O) must not exceed 100 characters",
            groups = ThirdOrder.class
    )
    private String careOf;

    @NotBlank(message = "Role is mandatory", groups = FirstOrder.class)
    @Pattern(
            regexp = "^(CUSTOMER|ADMIN)$",
            message = "Role must be either CUSTOMER or ADMIN",
            groups = SecondOrder.class
    )
    private String role;
}