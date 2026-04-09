package com.finsphere.common.dto.events;

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
    private Boolean isActive;
}
