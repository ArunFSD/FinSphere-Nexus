package com.finsphere.common.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * Ordered validation: presence checks run first, then format checks, then length checks. A field reports only
 * its first failing rule, which keeps error messages short and in a predictable order.
 * <p>
 * Usage: {@code @Validated(ValidationGroups.Sequence.class) @RequestBody SomeRequest request}
 */
public interface ValidationGroups {

    interface FirstOrder {
    }

    interface SecondOrder {
    }

    interface ThirdOrder {
    }

    @GroupSequence({ FirstOrder.class, SecondOrder.class, ThirdOrder.class, Default.class })
    interface Sequence {
    }
}
