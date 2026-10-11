package com.finsphere.common.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdTest {

    @Test
    void keepsWellFormedIncomingIds() {
        assertEquals("req-2026.10.09_abc", CorrelationId.sanitizeOrCreate("req-2026.10.09_abc"));
    }

    @Test
    void replacesMissingOrUnsafeIds() {
        for (String unsafe : new String[] { null, "", "short", "has space in it", "inject\nlog-line-123",
                "x".repeat(65) }) {
            String replaced = CorrelationId.sanitizeOrCreate(unsafe);
            assertNotEquals(unsafe, replaced);
            assertTrue(CorrelationId.isValid(replaced), replaced);
        }
    }

    @Test
    void generatedIdsAreValidAndUnique() {
        String first = CorrelationId.newId();
        assertTrue(CorrelationId.isValid(first));
        assertNotEquals(first, CorrelationId.newId());
        assertFalse(CorrelationId.isValid(null));
    }
}
