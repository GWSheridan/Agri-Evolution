package com.agrievolution.farming;

import java.util.List;

public final class HarvestExportPolicySelfTest {
    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var e = List.of(new HarvestExportPolicy.Candidate(0, 0, 40, 40),
                new HarvestExportPolicy.Candidate(2, 1, 64, 64),
                new HarvestExportPolicy.Candidate(4, 2, 16, 16));
        boolean[] enabled = {true, true, false};
        int[] ranks = {2, 1, 0};
        var result = HarvestExportPolicy.choose(e, enabled, ranks, 16, 0, 27);
        require(result != null && result.slot() == 0 && result.amount() == 24,
                "High priority first, reserve from item total");
        result = HarvestExportPolicy.choose(e, enabled, ranks, 40, 0, 27);
        require(result != null && result.slot() == 2 && result.amount() == 24,
                "Unexportable resource is skipped");
        enabled[0] = false;
        result = HarvestExportPolicy.choose(e, enabled, ranks, 0, 0, 27);
        require(result != null && result.slot() == 2, "Category filter excludes resources");
        enabled[1] = false;
        require(HarvestExportPolicy.choose(e, enabled, ranks, 0, 0, 27) == null,
                "Disabled seeds must not export");
        enabled[0] = true;
        enabled[1] = true;
        ranks[1] = 2;
        result = HarvestExportPolicy.choose(e, enabled, ranks, 0, 1, 27);
        require(result != null && result.slot() == 2, "Tie rotation chooses next slot");
        var same = List.of(new HarvestExportPolicy.Candidate(3, 0, 12, 32),
                new HarvestExportPolicy.Candidate(8, 0, 20, 32));
        result = HarvestExportPolicy.choose(same, enabled, ranks, 16, 3, 27);
        require(result != null && result.slot() == 3 && result.amount() == 12,
                "Reserve shared across exact-matching stacks");
        result = HarvestExportPolicy.choose(same, enabled, ranks, 32, 3, 27);
        require(result == null, "Do not export reserved stock");
        require(HarvestExportPolicy.NEAR_FULL_SLOTS == 21, "Correct overflow threshold");
        System.out.println("PASS: harvest export category filters, priorities, rotation, reserves, and overflow threshold");
    }
}
