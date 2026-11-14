package com.agrievolution.genetics;

/** Presentation-only rating; does not modify or rebalance the underlying crop genetics. */
public final class SeedQuality {
    private SeedQuality() {}

    public static int score(CropGenome genome) {
        return Math.round(genome.totalScore() / 3.0f);
    }

    public static String rating(CropGenome genome) {
        int score = score(genome);
        if (score >= 85) return "EXCEPTIONAL";
        if (score >= 65) return "SUPERIOR";
        if (score >= 45) return "STRONG";
        if (score >= 25) return "STANDARD";
        return "FRAGILE";
    }
}
