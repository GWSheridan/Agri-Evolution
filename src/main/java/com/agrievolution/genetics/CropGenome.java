package com.agrievolution.genetics;

import java.util.Objects;

/**
 * Compact inherited traits for an Agri Evolution crop line.
 *
 * Trait values are clamped to 0..100. Higher values are beneficial:
 * growth = faster maturation, yield = more produce, resilience = better tolerance.
 * This class intentionally has no Minecraft dependencies, so its rules are easy to test.
 */
public record CropGenome(int growth, int yield, int resilience) {
    public CropGenome {
        growth = clamp(growth);
        yield = clamp(yield);
        resilience = clamp(resilience);
    }

    public static CropGenome wild() {
        return new CropGenome(25, 25, 25);
    }

    public static CropGenome elite() {
        return new CropGenome(80, 75, 70);
    }

    public int totalScore() {
        return growth + yield + resilience;
    }

    public CropGenome breedWith(CropGenome other, long seed) {
        Objects.requireNonNull(other, "other");
        java.util.Random random = new java.util.Random(seed);
        return new CropGenome(
                inherit(growth, other.growth, random),
                inherit(yield, other.yield, random),
                inherit(resilience, other.resilience, random));
    }

    private static int inherit(int a, int b, java.util.Random random) {
        // Blend parental traits, add a small mutation, then clamp to valid range.
        int average = (a + b) / 2;
        int mutation = random.nextInt(11) - 5; // -5 through +5
        return clamp(average + mutation);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public String toString() {
        return "CropGenome{growth=" + growth + ", yield=" + yield
                + ", resilience=" + resilience + ", score=" + totalScore() + "}";
    }
}
