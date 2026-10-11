package com.finsphere.frontend.web.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.dto.chit.ChitCycleResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.security.AuthenticatedUser;

/**
 * Everything the scheme page shows, worked out from the service's answer for one viewer: the slot board, the
 * monthly ledger with the month that is open for its auction, the members and who can still win.
 *
 * @param plan          the scheme
 * @param slots         one tile per slot, in slot order
 * @param months        the monthly ledger in month order
 * @param openMonth     the first month not yet settled, {@code null} when every month is settled
 * @param members       members with their slots and wins (administrators see everyone, members see themselves)
 * @param freeSlots     slots nobody holds
 * @param winnerOptions members who can still win a month (administrators only)
 * @param mySlots       slots the viewer holds
 */
public record SchemeView(
        ChitPlanResponse plan,
        List<SlotTile> slots,
        List<MonthRow> months,
        @Nullable MonthRow openMonth,
        List<MemberRow> members,
        List<Integer> freeSlots,
        List<WinnerOption> winnerOptions,
        List<Integer> mySlots) {

    /**
     * @param number slot number
     * @param state  {@code free}, {@code taken} or {@code mine}
     * @param holder who holds it, when the viewer may know
     * @param prized whether the holder has already won with this slot
     * @param label  full description for screen readers and tooltips
     */
    public record SlotTile(int number, String state, @Nullable String holder, boolean prized, String label) {
    }

    /**
     * @param state  {@code settled}, {@code open} (the next auction) or {@code upcoming}
     * @param winner who won the month, when settled ("You" for the viewer)
     */
    public record MonthRow(int number, LocalDate opensOn, LocalDate closesOn, String state, @Nullable String winner,
            @Nullable Long winnerUserId, BigDecimal bid, BigDecimal dividend, BigDecimal payable) {

        public boolean settled() {
            return "settled".equals(this.state);
        }
    }

    public record MemberRow(long userId, String name, List<Integer> slots, long wins) {

        public boolean canWin() {
            return this.wins < this.slots.size();
        }
    }

    public record WinnerOption(long userId, String label) {
    }

    public boolean full() {
        return this.freeSlots.isEmpty();
    }

    public boolean finished() {
        return this.openMonth == null;
    }

    public long settledCount() {
        return this.months.stream().filter(MonthRow::settled).count();
    }

    public static SchemeView of(ChitPlanDetailResponse detail, AuthenticatedUser viewer) {
        ChitPlanResponse plan = detail.plan();
        boolean admin = viewer.isAdmin();

        Map<Long, String> names = new HashMap<>();
        Map<Integer, ChitEnrollmentResponse> bySlot = new HashMap<>();
        Map<Long, List<Integer>> slotsByUser = new LinkedHashMap<>();
        for (ChitEnrollmentResponse enrollment : detail.enrollments()) {
            names.putIfAbsent(enrollment.userId(), displayName(enrollment));
            bySlot.put(enrollment.slotNumber(), enrollment);
            slotsByUser.computeIfAbsent(enrollment.userId(), (id) -> new ArrayList<>()).add(enrollment.slotNumber());
        }
        slotsByUser.values().forEach((slots) -> slots.sort(Comparator.naturalOrder()));

        Map<Long, Long> wins = new HashMap<>();
        for (ChitCycleResponse cycle : detail.cycles()) {
            if (cycle.closed() && cycle.winnerUserId() != null) {
                wins.merge(cycle.winnerUserId(), 1L, Long::sum);
            }
        }

        // A member who has won w times is shown as prized on the first w of their slots
        Set<Integer> prizedSlots = new HashSet<>();
        slotsByUser.forEach((userId, slots) -> {
            long won = Math.min(wins.getOrDefault(userId, 0L), slots.size());
            for (int i = 0; i < won; i++) {
                prizedSlots.add(slots.get(i));
            }
        });

        Set<Integer> taken = new TreeSet<>(detail.takenSlots());
        taken.addAll(bySlot.keySet());
        List<Integer> mySlots = slotsByUser.getOrDefault(viewer.userId(), List.of());

        List<SlotTile> tiles = new ArrayList<>(plan.totalMembers());
        List<Integer> free = new ArrayList<>();
        for (int slot = 1; slot <= plan.totalMembers(); slot++) {
            ChitEnrollmentResponse holder = bySlot.get(slot);
            boolean prized = prizedSlots.contains(slot);
            if (mySlots.contains(slot)) {
                tiles.add(new SlotTile(slot, "mine", "You", prized,
                        "Slot " + slot + ", yours" + (prized ? ", prized" : "")));
            }
            else if (taken.contains(slot)) {
                String name = (holder != null) ? displayName(holder) : null;
                tiles.add(new SlotTile(slot, "taken", name, prized, "Slot " + slot + ", taken"
                        + ((name != null) ? " by " + name : "") + (prized ? ", prized" : "")));
            }
            else {
                free.add(slot);
                tiles.add(new SlotTile(slot, "free", null, false, "Slot " + slot + ", free"));
            }
        }

        List<MonthRow> months = new ArrayList<>(detail.cycles().size());
        MonthRow open = null;
        List<ChitCycleResponse> cycles = new ArrayList<>(detail.cycles());
        cycles.sort(Comparator.comparing(ChitCycleResponse::monthlyCount));
        for (ChitCycleResponse cycle : cycles) {
            String state;
            if (cycle.closed()) {
                state = "settled";
            }
            else if (open == null) {
                state = "open";
            }
            else {
                state = "upcoming";
            }
            String winner = null;
            if (cycle.closed() && cycle.winnerUserId() != null) {
                if (cycle.winnerUserId().equals(viewer.userId())) {
                    winner = "You";
                }
                else if (admin) {
                    winner = names.getOrDefault(cycle.winnerUserId(), "Member " + cycle.winnerUserId());
                }
                else {
                    winner = "Another member";
                }
            }
            MonthRow row = new MonthRow(cycle.monthlyCount(), cycle.paymentWindowStart(),
                    cycle.paymentWindowDeadline(), state, winner, cycle.winnerUserId(),
                    Objects.requireNonNullElse(cycle.auctionWinnerBid(), BigDecimal.ZERO),
                    Objects.requireNonNullElse(cycle.dividendPerMember(), BigDecimal.ZERO),
                    cycle.monthlyPayableAmount());
            if ("open".equals(state)) {
                open = row;
            }
            months.add(row);
        }

        List<MemberRow> members = new ArrayList<>();
        List<WinnerOption> winnerOptions = new ArrayList<>();
        slotsByUser.forEach((userId, slots) -> {
            MemberRow member = new MemberRow(userId, names.get(userId), List.copyOf(slots),
                    wins.getOrDefault(userId, 0L));
            members.add(member);
            if (admin && member.canWin()) {
                winnerOptions.add(new WinnerOption(userId, winnerLabel(member)));
            }
        });
        members.sort(Comparator.comparing((MemberRow member) -> member.slots().getFirst()));
        winnerOptions.sort(Comparator.comparing(WinnerOption::label, String.CASE_INSENSITIVE_ORDER));

        return new SchemeView(plan, List.copyOf(tiles), List.copyOf(months), open, List.copyOf(members),
                List.copyOf(free), List.copyOf(winnerOptions), List.copyOf(mySlots));
    }

    private static String displayName(ChitEnrollmentResponse enrollment) {
        String name = enrollment.memberName();
        return (name == null || name.isBlank()) ? "Member " + enrollment.userId() : name;
    }

    private static String winnerLabel(MemberRow member) {
        String slots = (member.slots().size() == 1) ? "slot " + member.slots().getFirst()
                : "slots " + String.join(", ", member.slots().stream().map(String::valueOf).toList());
        return member.name() + " (" + slots + ((member.wins() > 0) ? ", won " + member.wins() + " so far" : "")
                + ")";
    }
}
