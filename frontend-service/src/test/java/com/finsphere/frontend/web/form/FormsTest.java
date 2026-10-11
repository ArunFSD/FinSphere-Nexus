package com.finsphere.frontend.web.form;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.finsphere.common.dto.auth.RegistrationRequest;
import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormsTest {

    @Test
    void mobileNumbersAreReducedToTenDigits() {
        assertEquals("9876543210", RegistrationForm.mobileNumber("+91 98765 43210"));
        assertEquals("9876543210", RegistrationForm.mobileNumber("098765-43210"));
        assertEquals("9876543210", RegistrationForm.mobileNumber("9876543210"));
        assertEquals("abc", RegistrationForm.mobileNumber(" abc "));
        assertNull(RegistrationForm.mobileNumber(null));
    }

    @Test
    void registrationTrimsAndDropsBlankOptionalFields() {
        RegistrationForm form = new RegistrationForm(" Priya Raman ", "+91 98765 01234", "  ", "Secret123",
                "Secret123", " Mylapore ", "", "ADMIN");
        assertTrue(form.passwordsMatch());
        RegistrationRequest request = form.toRequest("CUSTOMER");
        assertEquals("Priya Raman", request.fullName());
        assertEquals("9876501234", request.phoneNumber());
        assertNull(request.email());
        assertNull(request.careOf());
        assertEquals("Mylapore", request.address());
        assertEquals("CUSTOMER", request.role());
        assertEquals("", form.withoutPasswords().password());
        assertFalse(new RegistrationForm("a", "b", null, "x", "y", "c", null, null).passwordsMatch());
    }

    @Test
    void paymentTargetCombinesTypeAndId() {
        LocalDate today = LocalDate.of(2026, 10, 9);
        PaymentForm form = new PaymentForm(7L, PaymentForm.target(TransactionType.LOAN_PAYMENT, 12), BigDecimal.TEN,
                today, PaymentMode.UPI, " Selvam ", "  ");
        TransactionRequest request = form.toRequest();
        assertEquals(TransactionType.LOAN_PAYMENT, request.transactionType());
        assertEquals(12L, request.targetId());
        assertEquals("Selvam", request.receivedByInfo());
        assertNull(request.remarks());

        for (String bad : new String[] { null, "", "LOAN_PAYMENT", "SALARY:1", "CHIT_PAYMENT:x", ":5" }) {
            assertNull(new PaymentForm(7L, bad, BigDecimal.TEN, today, PaymentMode.CASH, "x", null).toRequest(), bad);
        }
    }
}
