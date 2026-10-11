package com.finsphere.common.dto.finance;

/**
 * Days on which no daily loan installment is collected.
 */
public enum LeaveType {

    /** Collect every day. */
    NONE,

    /** Skip Sundays. */
    SUNDAY,

    /** Skip Saturdays and Sundays. */
    WEEKEND
}
