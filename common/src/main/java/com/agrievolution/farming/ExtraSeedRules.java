package com.agrievolution.farming;

/** Shared extra seed chance for both hand-harvested and greenhouse-harvested crops.
 * A mature crop always retains its existing base seed behaviour. These rules
 * govern exactly ONE additional, genome-preserving seed roll per harvest.
 */
public final class ExtraSeedRules {
    private ExtraSeedRules() { }

    public static int chancePercent(int yield, boolean abundant) {
        // Ordinary seeds: 20%; stronger Yield genetics and Abundant traits help.
        // Caps prevent the 27-slot store/seed bank from exploding with endless seeds.
        int bonus = Math.max(0, Math.min(100, yield) - 25) * 2 / 5;
        return Math.min(60, 20 + bonus + (abundant ? 10 : 0));
    }
}
