package com.agrievolution.farming;

import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.SpeciesBreeder;

/** Dependency-free deterministic checks for the v0.6.0 farming balance. */
public final class FarmingSelfTest {
    private FarmingSelfTest() {}
    public static void main(String[] args) {
        expect(FarmEnvironment.IRRIGATION_RANGE == 6, "permanent basin radius beyond vanilla water");
        expect(FarmEnvironment.clampNutrition(-4) == 0, "minimum nutrition");
        expect(FarmEnvironment.clampNutrition(120) == 100, "maximum nutrition");
        int bare = FarmEnvironment.growthChance(25, 40, 0, false, false);
        int prepared = FarmEnvironment.growthChance(25, 70, 7, true, true);
        expect(prepared > bare && prepared <= 180, "farm boosts improve growth without guaranteeing it");
        expect(FarmEnvironment.growthChance(100, 100, 7, true, true) <= 180, "growth cap");
        int without = 0, with = 0;
        for (long n = 0; n < 10000; n++) {
            if (SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, n)
                    == CropSpecies.VIOLET_STARFLOWER) without++;
            if (SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, n, 5)
                    == CropSpecies.VIOLET_STARFLOWER) with++;
        }
        expect(without > 2400 && without < 2600, "original 25% mutation");
        expect(with > 2900 && with < 3100, "supported 30% mutation");
        expect(SpeciesBreeder.cross(CropSpecies.WILD, CropSpecies.WILD, 12, 5)
                == CropSpecies.WILD, "no new mutations from same species");
        System.out.println("Agri Evolution farming tests passed: " + bare + " -> " + prepared
                + " growth chance; 25% recipe " + without + "/10000 -> " + with + "/10000 with supports.");
    }
    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
