package com.agrievolution.genetics;

import java.util.Objects;

/**
 * Read-only predictions based on the same inheritance rule as CropGenome.breedWith.
 * These predictions NEVER generate a seed, consume an item, or change the RNG.
 */
public final class SeedBreedingGuide {
    private SeedBreedingGuide() {}

    public static CropGenome expected(CropGenome a, CropGenome b) {
        Objects.requireNonNull(a, "parent A");
        Objects.requireNonNull(b, "parent B");
        return new CropGenome((a.growth() + b.growth()) / 2,
                (a.yield() + b.yield()) / 2,
                (a.resilience() + b.resilience()) / 2);
    }

    /** Each offspring trait can vary by -5..+5 around the truncated midpoint. */
    public static CropGenome minimum(CropGenome a, CropGenome b) {
        CropGenome mid = expected(a, b);
        return new CropGenome(mid.growth() - 5, mid.yield() - 5, mid.resilience() - 5);
    }

    public static CropGenome maximum(CropGenome a, CropGenome b) {
        CropGenome mid = expected(a, b);
        return new CropGenome(mid.growth() + 5, mid.yield() + 5, mid.resilience() + 5);
    }

    /** Identify the lowest projected trait for an actionable next breeding target. */
    public static String weakestTrait(CropGenome a, CropGenome b) {
        CropGenome mid = expected(a, b);
        if (mid.growth() <= mid.yield() && mid.growth() <= mid.resilience()) return "GROWTH";
        if (mid.yield() <= mid.resilience()) return "YIELD";
        return "RESILIENCE";
    }

    public static String strongerSeed(CropGenome a, CropGenome b) {
        int aScore = SeedQuality.score(a);
        int bScore = SeedQuality.score(b);
        return aScore == bScore ? "EVEN" : aScore > bScore ? "A" : "B";
    }
}
