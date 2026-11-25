package com.agrievolution.farming;

import java.util.List;

/** Deterministic slot planner: priority, optional genetics, then round-robin fairness.
 *  All eligibility checks (filters, enabled slots, reserves) happen server-side before calling. */
public final class SeedBankSelection {
    public record Candidate(int slot, int priority, int genomeScore) { }

    private SeedBankSelection() { }

    public static int choose(List<Candidate> eligible, int startCursor, int slotCount, boolean bestGenetics) {
        if (slotCount <= 0 || eligible.isEmpty()) return -1;
        int bestSlot = -1;
        int bestPriority = Integer.MIN_VALUE;
        int bestScore = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (Candidate candidate : eligible) {
            if (candidate.slot() < 0 || candidate.slot() >= slotCount) continue;
            int distance = Math.floorMod(candidate.slot() - startCursor, slotCount);
            if (candidate.priority() > bestPriority
                    || (candidate.priority() == bestPriority && bestGenetics && candidate.genomeScore() > bestScore)
                    || (candidate.priority() == bestPriority && (!bestGenetics || candidate.genomeScore() == bestScore)
                    && distance < bestDistance)) {
                bestSlot = candidate.slot();
                bestPriority = candidate.priority();
                bestScore = candidate.genomeScore();
                bestDistance = distance;
            }
        }
        return bestSlot;
    }
    /** Automatic replanting ranks genetics first, using priorities only to break ties. */
    public static int chooseStrongest(List<Candidate> eligible, int startCursor, int slotCount) {
        if (slotCount <= 0) return -1;
        int bestSlot = -1, bestGenes = Integer.MIN_VALUE, bestPriority = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (Candidate candidate : eligible) {
            if (candidate.slot() < 0 || candidate.slot() >= slotCount) continue;
            int distance = Math.floorMod(candidate.slot() - startCursor, slotCount);
            if (candidate.genomeScore() > bestGenes
                    || (candidate.genomeScore() == bestGenes && candidate.priority() > bestPriority)
                    || (candidate.genomeScore() == bestGenes && candidate.priority() == bestPriority
                        && distance < bestDistance)) {
                bestSlot = candidate.slot();
                bestGenes = candidate.genomeScore();
                bestPriority = candidate.priority();
                bestDistance = distance;
            }
        }
        return bestSlot;
    }
}
