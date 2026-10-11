package com.finsphere.frontend.web.support;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class NoticeTest {

    @Test
    void onlyKnownKeysProduceAMessage() {
        assertEquals("Slot booked.", Notice.messageFor("slot-booked"));
        assertSame(Notice.FORM_EXPIRED, Notice.of("form-expired"));
        assertNull(Notice.messageFor("<script>alert(1)</script>"));
        assertNull(Notice.of(null));
    }

    @Test
    void problemsDoNotLookLikeSuccess() {
        assertEquals("alert-error", Notice.FORM_EXPIRED.tone().alertClass());
        assertEquals("#i-alert", Notice.FORM_EXPIRED.tone().icon());
        assertEquals("alert-info", Notice.SESSION_ENDED.tone().alertClass());
        assertEquals("alert-success", Notice.PAYMENT_RECORDED.tone().alertClass());
    }

    @Test
    void keysAreUniqueAndMessagesUseNoSemicolons() {
        Set<String> keys = Arrays.stream(Notice.values()).map(Notice::key).collect(Collectors.toSet());
        assertEquals(Notice.values().length, keys.size());
        for (Notice notice : Notice.values()) {
            assertFalse(notice.message().contains(";"), notice::name);
        }
    }
}
