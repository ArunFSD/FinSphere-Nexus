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
    private String fullName;

    @NotBlank(
            message = "Phone number is mandatory",
            groups = FirstOrder.class
    )
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid Indian mobile number",
            groups = SecondOrder.class
    )
    private String phoneNumber;

    @Email(
            message = "Invalid email format",
            groups = SecondOrder.class
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
    private String password;

    @NotBlank(
            message = "Address is mandatory",
            groups = FirstOrder.class
    )
    private String address;

    @Pattern(
            regexp = "^[a-zA-Z\\s]*$",
            message = "Care of (C/O) can only contain letters and spaces",
            groups = SecondOrder.class
    )
    private String careOf;
}