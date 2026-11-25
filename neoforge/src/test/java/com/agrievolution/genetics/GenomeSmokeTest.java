package com.agrievolution.genetics;

/** Standalone deterministic genetics checks; run with `gradlew genomeSelfTest`. */
public final class GenomeSmokeTest {
    public static void main(String[] args) {
        CropGenome parentA = CropGenome.wild();
        CropGenome parentB = CropGenome.elite();
        CropGenome child = GenomeBreeder.cross(parentA, parentB, 12345L);
        if (child.growth() < 0 || child.growth() > 100) throw new AssertionError("growth out of range");
        if (child.yield() < 0 || child.yield() > 100) throw new AssertionError("yield out of range");
        if (child.resilience() < 0 || child.resilience() > 100) throw new AssertionError("resilience out of range");
        if (!child.equals(GenomeBreeder.cross(parentA, parentB, 12345L))) throw new AssertionError("seed should be reproducible");
        if (!new CropGenome(-99, 888, 13).equals(new CropGenome(0, 100, 13)))
            throw new AssertionError("trait clamping failed");
        for (long seed = 0; seed < 2000; seed++) {
            CropGenome g = GenomeBreeder.cross(parentA, parentB, seed);
            if (g.growth() < 0 || g.growth() > 100 || g.yield() < 0 || g.yield() > 100
                    || g.resilience() < 0 || g.resilience() > 100)
                throw new AssertionError("invalid inherited trait for seed " + seed);
        }
        System.out.println("Agri Evolution genetics self-test PASSED: " + child);
    }
}
