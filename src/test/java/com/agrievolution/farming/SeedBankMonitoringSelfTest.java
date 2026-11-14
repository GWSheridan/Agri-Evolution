package com.agrievolution.farming;

import java.util.List;

/** Compile/run independently from NeoForge: monitoring must not change seed selection. */
public final class SeedBankMonitoringSelfTest {
    public static void main(String[] args) {
        var empty = SeedBankMonitoring.summarize(List.of());
        check(empty.present() == 0 && SeedBankMonitoring.noEligibleReason(empty) == SeedBankMonitoring.EMPTY_BANK);
        var mixed = SeedBankMonitoring.summarize(List.of(
                new SeedBankMonitoring.Slot(true, false, true, true),
                new SeedBankMonitoring.Slot(true, true, false, true),
                new SeedBankMonitoring.Slot(true, true, true, false),
                new SeedBankMonitoring.Slot(true, true, true, true),
                new SeedBankMonitoring.Slot(false, true, true, true)));
        check(mixed.present() == 4 && mixed.eligible() == 1 && mixed.disabled() == 1
                && mixed.filtered() == 1 && mixed.reserved() == 1);
        var protectedSeeds = SeedBankMonitoring.summarize(List.of(
                new SeedBankMonitoring.Slot(true, true, true, false)));
        check(SeedBankMonitoring.noEligibleReason(protectedSeeds) == SeedBankMonitoring.RESERVED);
        var filteredSeeds = SeedBankMonitoring.summarize(List.of(
                new SeedBankMonitoring.Slot(true, true, false, true)));
        check(SeedBankMonitoring.noEligibleReason(filteredSeeds) == SeedBankMonitoring.FILTERED);
        var disabledSeeds = SeedBankMonitoring.summarize(List.of(
                new SeedBankMonitoring.Slot(true, false, true, true)));
        check(SeedBankMonitoring.noEligibleReason(disabledSeeds) == SeedBankMonitoring.DISABLED);
        var mixedExclusions = SeedBankMonitoring.summarize(List.of(
                new SeedBankMonitoring.Slot(true, false, true, true),
                new SeedBankMonitoring.Slot(true, true, false, true)));
        check(SeedBankMonitoring.noEligibleReason(mixedExclusions) == SeedBankMonitoring.MIXED_EXCLUSIONS);
        check(SeedBankMonitoring.efficiencyPercent(4, 3) == 75);
        check(SeedBankMonitoring.efficiencyPercent(0, 0) == 0);
        check(SeedBankMonitoring.efficiencyPercent(3, 5) == 100);
        System.out.println("Agri Evolution 0.23.0: Smart greenhouse monitoring planner tests passed");
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("SeedBankMonitoring regression");
    }
}
