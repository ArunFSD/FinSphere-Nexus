package com.finsphere.frontend.web.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.finsphere.common.dto.chit.ChitCycleResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.frontend.web.view.SchemeView.SlotTile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemeViewTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "9000000001", "ADMIN", "Admin", null);

    private static final AuthenticatedUser RAVI = new AuthenticatedUser(7L, "9000000007", "CUSTOMER", "Ravi", null);

    private static final ChitPlanResponse PLAN = new ChitPlanResponse(1L, "Gold", new BigDecimal("100000"), 4,
            new BigDecimal("5"), new BigDecimal("5000"), new BigDecimal("25000"), new BigDecimal("40000"), 5, 4, 2,
            10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1), true, null);

    /** Ravi (7) holds slots 1 and 4 and won month 1; Lakshmi (8) holds slot 2 and won month 2; slot 3 is held by
     * someone a customer cannot see; slot 5 is free. */
    private static ChitPlanDetailResponse detail(boolean adminView) {
        List<ChitEnrollmentResponse> enrollments = new ArrayList<>();
        enrollments.add(enrollment(1, 7L, "Ravi Kumar"));
        enrollments.add(enrollment(4, 7L, "Ravi Kumar"));
        if (adminView) {
            enrollments.add(enrollment(2, 8L, "Lakshmi Narayanan"));
            enrollments.add(enrollment(3, 9L, null));
        }
        List<ChitCycleResponse> cycles = List.of(cycle(3, false, null), cycle(1, true, 7L), cycle(2, true, 8L),
                cycle(4, false, null));
        return new ChitPlanDetailResponse(PLAN, cycles, enrollments, List.of(1, 2, 3, 4));
    }

    private static ChitEnrollmentResponse enrollment(int slot, long userId, String name) {
        return new ChitEnrollmentResponse((long) slot, 1L, "Gold", userId, name, slot, true, Instant.EPOCH);
    }

    private static ChitCycleResponse cycle(int month, boolean closed, Long winner) {
        LocalDate opens = LocalDate.of(2026, month, 1);
        return new ChitCycleResponse((long) month, 1L, month, opens, opens.plusDays(10),
                closed ? new BigDecimal("20000") : BigDecimal.ZERO, closed ? new BigDecimal("750") : BigDecimal.ZERO,
                closed ? new BigDecimal("24250") : new BigDecimal("25000"), winner, closed);
    }

    @Test
    void administratorsSeeEveryHolderAndWhoCanStillWin() {
        SchemeView view = SchemeView.of(detail(true), ADMIN);

        assertEquals(List.of("taken", "taken", "taken", "taken", "free"),
                view.slots().stream().map(SlotTile::state).toList());
        assertTrue(view.slots().get(0).prized(), "Ravi won once: his first slot is prized");
        assertFalse(view.slots().get(3).prized(), "his second slot has not won yet");
        assertTrue(view.slots().get(1).prized());
        assertEquals("Member 9", view.slots().get(2).holder());
        assertEquals(List.of(5), view.freeSlots());

        assertEquals(List.of(1, 2, 3, 4), view.months().stream().map(SchemeView.MonthRow::number).toList());
        assertEquals(3, view.openMonth().number());
        assertEquals("upcoming", view.months().get(3).state());
        assertEquals("Ravi Kumar", view.months().get(0).winner());
        assertEquals(2, view.settledCount());

        // Ravi can win again (2 slots, 1 win), Lakshmi cannot (1 slot, 1 win), member 9 can
        assertEquals(List.of("Member 9 (slot 3)", "Ravi Kumar (slots 1, 4, won 1 so far)"),
                view.winnerOptions().stream().map(SchemeView.WinnerOption::label).toList());
    }

    @Test
    void membersSeeTheirOwnSlotsAndNoOtherNames() {
        SchemeView view = SchemeView.of(detail(false), RAVI);

        assertEquals(List.of("mine", "taken", "taken", "mine", "free"),
                view.slots().stream().map(SlotTile::state).toList());
        assertNull(view.slots().get(1).holder());
        assertEquals(List.of(1, 4), view.mySlots());
        assertEquals("You", view.months().get(0).winner());
        assertEquals("Another member", view.months().get(1).winner());
        assertTrue(view.winnerOptions().isEmpty(), "only administrators settle auctions");
        assertFalse(view.full());
        assertFalse(view.finished());
    }
}
