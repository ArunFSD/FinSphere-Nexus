package com.finsphere.dto;

import com.finsphere.common.validation.ValidationGroups.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotNull(
            message = "Plan ID is mandatory",
            groups = FirstOrder.class
    )
    private Long planId;

    @NotNull(
            message = "User ID is mandatory",
            groups = FirstOrder.class
    )
    private Long userId;

    @NotNull(
            message = "Slot Number is mandatory",
            groups = FirstOrder.class
    )
    @Min(
            value = 1,
            message = "Slot Number must be at least 1",
            groups = SecondOrder.class
    )
    private Integer slotNumber;
}