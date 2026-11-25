package com.agrievolution.genetics;

/** Standalone regression test for workstation genetics; run `gradlew breedingStationSelfTest`. */
public final class SeedBreedingStationSelfTest {
    public static void main(String[] args) {
        CropGenome a = new CropGenome(35, 51, 44);
        CropGenome b = new CropGenome(81, 20, 70);
        CropGenome low = SeedBreedingGuide.minimum(a, b);
        CropGenome high = SeedBreedingGuide.maximum(a, b);
        for (int attempt = 0; attempt < 10000; attempt++) {
            CropGenome offspring = GenomeBreeder.cross(a, b, attempt);
            require(offspring.growth() >= low.growth() && offspring.growth() <= high.growth(), "growth outside preview");
            require(offspring.yield() >= low.yield() && offspring.yield() <= high.yield(), "yield outside preview");
            require(offspring.resilience() >= low.resilience() && offspring.resilience() <= high.resilience(), "resilience outside preview");
        }
        var recipe = SpeciesBreeder.mutationRecipe(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX);
        require(recipe != null && recipe.offspring() == CropSpecies.VIOLET_STARFLOWER,
                "station must use existing Violet Starflower mutation");
        require(recipe.chanceWithSupports(0) == 25 && recipe.chanceWithSupports(5) == 30,
                "2 wooden supports must add exactly +5 percentage points");
        for (int roll : new int[]{30, 33, 36, 37}) {
            long seed = 0;
            while (new java.util.Random(seed).nextInt(100) != roll) seed++;
            for (int bonus : new int[]{5, 8, 12}) {
                var child = SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed, bonus);
                require((child == CropSpecies.VIOLET_STARFLOWER) == (roll < 25 + bonus),
                        "Station roll must match the preview for support bonus " + bonus);
            }
            var pollinated = SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed, 12, 3);
            require(pollinated == CropSpecies.VIOLET_STARFLOWER, "Pollination stacks with obsidian support");
        }
        require(SpeciesBreeder.mutationRecipe(CropSpecies.WILD, CropSpecies.WILD) == null,
                "same-species breeding must not create arbitrary mutations");
        require(SeedQuality.score(new CropGenome(100, 100, 100)) == 100, "score changed");
        System.out.println("Agri Evolution 0.9.0 breeding station genetics self-test PASSED");
    }
    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
