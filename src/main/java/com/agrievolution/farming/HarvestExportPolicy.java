package com.agrievolution.farming;

import java.util.List;

/** Pure Java priority / reserve selection. No crop, texture or genetics code is changed. */
public final class HarvestExportPolicy {
    public static final int RESOURCES = 0, BOTANICAL = 1, SEEDS = 2;
    public static final int NEAR_FULL_SLOTS = 21;
    public record Candidate(int slot, int category, int count, int totalMatching) { }
    public record Selection(int slot, int amount) { }
    private HarvestExportPolicy() { }

    /** High (2) first; tie-break by circular slot order. Keep the requested reserve
     * across all exact-matching stacks, not just the stack currently examined. */
    public static Selection choose(List<Candidate> candidates, boolean[] enabled,
                                   int[] priorities, int reserve, int cursor, int slotCount) {
        Candidate best = null;
        int quantity = 0;
        for (Candidate candidate : candidates) {
            int cat = candidate.category();
            if (candidate.slot() < 0 || candidate.slot() >= slotCount || cat < 0 || cat >= 3
                    || !enabled[cat] || candidate.count() <= 0) continue;
            int allowed = Math.min(candidate.count(), Math.max(0, candidate.totalMatching() - reserve));
            if (allowed == 0) continue;
            if (best == null || priorities[cat] > priorities[best.category()]
                    || (priorities[cat] == priorities[best.category()]
                        && Math.floorMod(candidate.slot() - cursor, slotCount)
                        < Math.floorMod(best.slot() - cursor, slotCount))) {
                best = candidate;
                quantity = allowed;
            }
        }
        return best == null ? null : new Selection(best.slot(), quantity);
    }
}
