package com.finsphere.common.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateEvent implements Serializable {
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String careOf;
    private String role;
    private Boolean isActive;
}
