package com.agrievolution.genetics;

import java.util.Objects;

/** High-level breeding helpers used by future crop blocks and breeding stations. */
public final class GenomeBreeder {
    private GenomeBreeder() {}

    /**
     * Crosses two crop lines. The world seed and breeding attempt number can be mixed
     * into attemptSeed so each attempt is reproducible for debugging.
     */
    public static CropGenome cross(CropGenome parentA, CropGenome parentB, long attemptSeed) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        return parentA.breedWith(parentB, attemptSeed);
    }

    /** A simple discovery threshold for later progression and journal integration. */
    public static boolean isExceptional(CropGenome genome) {
        Objects.requireNonNull(genome, "genome");
        return genome.totalScore() >= 210;
    }
}
