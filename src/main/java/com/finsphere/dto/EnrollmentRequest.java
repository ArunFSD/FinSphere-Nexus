package com.finsphere.dto;

import lombok.Data;

@Data
public class EnrollmentRequest {

    private Long planId;
    private Long userId;
    private Integer slotNumber;

}
