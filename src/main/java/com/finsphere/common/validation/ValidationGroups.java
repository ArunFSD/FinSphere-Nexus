package com.finsphere.common.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

// 1. Define markers for the order
public interface ValidationGroups {

    interface FirstOrder {}
    interface SecondOrder {}
    interface ThirdOrder {}

    // 2. Define the Sequence: First check FirstOrder, then SecondOrder, then anything else
    @GroupSequence({FirstOrder.class, SecondOrder.class, ThirdOrder.class, Default.class})
    interface Sequence {}
}
