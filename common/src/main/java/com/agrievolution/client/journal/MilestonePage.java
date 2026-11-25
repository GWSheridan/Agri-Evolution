package com.agrievolution.client.journal;

import com.agrievolution.content.FarmingMilestones;
import java.util.ArrayList;
import java.util.List;

public final class MilestonePage {
    private static int mask;

    public static void receive(int completed) { mask = completed; }

    static String[] progress(String title) {
        if (FarmingMilestones.CROP_PAGE.equals(title)) return lines(List.of(FarmingMilestones.CROSS));
        if (FarmingMilestones.MACHINE_PAGE.equals(title)) return lines(List.of(FarmingMilestones.SOIL,
                FarmingMilestones.RESERVOIR, FarmingMilestones.PRESERVE, FarmingMilestones.GREENHOUSE));
        return null;
    }

    private static String[] lines(List<FarmingMilestones> steps) {
        var lines = new ArrayList<String>();
        boolean next = true;
        for (var step : steps) {
            boolean done = step.reached(mask);
            lines.add((done ? "+ " : next ? "> " : "- ") + step.instruction());
            if (!done) next = false;
        }
        lines.add(next ? "All milestones complete" : "> Next milestone");
        return lines.toArray(String[]::new);
    }

    private MilestonePage() {}
}
