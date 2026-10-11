package com.finsphere.frontend.web.form;

import org.jspecify.annotations.Nullable;

/** Book a slot: members book for themselves, administrators choose the member. */
public record SlotForm(@Nullable Integer slotNumber, @Nullable Long userId) {

    public static final SlotForm EMPTY = new SlotForm(null, null);
}
