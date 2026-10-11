package com.finsphere.common.dto.finance;

/**
 * What a collected payment settles.
 */
public enum TransactionType {

    /** Monthly installment of a chit scheme; the target is the plan id. */
    CHIT_PAYMENT,

    /** Daily installment of a loan; the target is the loan id. */
    LOAN_PAYMENT
}
