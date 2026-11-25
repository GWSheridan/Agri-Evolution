package com.agrievolution.farming;

import java.util.List;

/** Read-only diagnostics for the existing server-authoritative nine-slot Seed Bank.
 * No effect on the planting planner, genetics, priority, or reserve calculations. */
public final class SeedBankMonitoring {
    public static final int NONE = 0;
    public static final int PLANTED = 1;
    public static final int EMPTY_BANK = 2;
    public static final int DISABLED = 3;
    public static final int RESERVED = 4;
    public static final int FILTERED = 5;
    public static final int NO_PLOTS = 6;
    public static final int PLACEMENT_FAILED = 7;
    public static final int MIXED_EXCLUSIONS = 8;

    public record Slot(boolean present, boolean enabled, boolean categoryAllowed, boolean aboveReserve) { }
    public record Summary(int present, int eligible, int disabled, int reserved, int filtered) { }

    private SeedBankMonitoring() { }

    /** Counts each occupied seed slot once, classified by the first reason it is excluded.
     * Disabled, category filters, and reserve are checked in the planner's exact order. */
    public static Summary summarize(List<Slot> slots) {
        int present = 0, eligible = 0, disabled = 0, reserved = 0, filtered = 0;
        for (Slot slot : slots) {
            if (!slot.present()) continue;
            present++;
            if (!slot.enabled()) disabled++;
            else if (!slot.categoryAllowed()) filtered++;
            else if (!slot.aboveReserve()) reserved++;
            else eligible++;
        }
        return new Summary(present, eligible, disabled, reserved, filtered);
    }

    public static int noEligibleReason(Summary snapshot) {
        if (snapshot.eligible() > 0) return NONE;
        if (snapshot.present() == 0) return EMPTY_BANK;
        if (snapshot.disabled() == snapshot.present()) return DISABLED;
        if (snapshot.filtered() == snapshot.present()) return FILTERED;
        if (snapshot.reserved() == snapshot.present()) return RESERVED;
        return MIXED_EXCLUSIONS;
    }

    public static int efficiencyPercent(int attempts, int planted) {
        if (attempts <= 0) return 0;
        return (int) Math.min(100L, Math.max(0L, 100L * planted / attempts));
    }
}
