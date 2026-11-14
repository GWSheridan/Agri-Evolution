package com.agrievolution.farming;

import java.util.List;

/** Standalone test runnable with javac/java (no Minecraft classes needed). */
public final class SeedBankPlannerSelfTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var seeds = List.of(new SeedBankSelection.Candidate(0, 1, 75),
                new SeedBankSelection.Candidate(1, 2, 60),
                new SeedBankSelection.Candidate(2, 2, 240),
                new SeedBankSelection.Candidate(3, 0, 300));
        require(SeedBankSelection.choose(seeds, 0, 9, false) == 1, "higher priority first");
        require(SeedBankSelection.choose(seeds, 0, 9, true) == 2, "strongest genetics wins priority tie");
        require(SeedBankSelection.choose(seeds, 2, 9, false) == 2, "round-robin cursor");
        require(SeedBankSelection.choose(seeds, 3, 9, false) == 1, "round-robin wraps");
        require(SeedBankSelection.choose(List.of(), 0, 9, true) == -1, "empty bank");
        require(SeedBankSelection.choose(List.of(new SeedBankSelection.Candidate(6, 1, 75)), 0, 9, true) == 6,
                "filtered slots are excluded");
        require(SeedBankSelection.choose(List.of(new SeedBankSelection.Candidate(9, 2, 300)), 0, 9, true) == -1,
                "invalid slot excluded");
        System.out.println("PASS: Advanced Seed Bank priority, genes, fairness and exclusion");
    }
}
