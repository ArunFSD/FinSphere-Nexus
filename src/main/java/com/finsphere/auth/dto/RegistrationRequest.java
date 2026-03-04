package com.finsphere.auth.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    // Auth info
    private String phoneNumber;
    private String password;
    private String email;

    // Profile info
    private String fullName;
    private String careOf;
    private String address;
}
