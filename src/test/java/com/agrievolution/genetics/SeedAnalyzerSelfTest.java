package com.agrievolution.genetics;

/** Run with gradlew seedAnalyzerSelfTest; no world or Minecraft client is required. */
public final class SeedAnalyzerSelfTest {
    public static void main(String[] args) {
        testGrades();
        testComparison();
        testOffspringBounds();
        testRecipes();
        System.out.println("Agri Evolution 0.8.0 Seed Analyzer self-test PASSED");
    }

    private static void testGrades() {
        assertRating(0, "FRAGILE");
        assertRating(24, "FRAGILE");
        assertRating(25, "STANDARD");
        assertRating(44, "STANDARD");
        assertRating(45, "STRONG");
        assertRating(64, "STRONG");
        assertRating(65, "SUPERIOR");
        assertRating(84, "SUPERIOR");
        assertRating(85, "EXCEPTIONAL");
        assertRating(100, "EXCEPTIONAL");
    }

    private static void assertRating(int score, String grade) {
        CropGenome genome = new CropGenome(score, score, score);
        require(SeedQuality.score(genome) == score, "quality score mismatch");
        require(SeedQuality.rating(genome).equals(grade), "grade mismatch at " + score);
    }

    private static void testComparison() {
        CropGenome a = new CropGenome(15, 60, 30);
        CropGenome b = new CropGenome(45, 22, 65);
        require(SeedBreedingGuide.strongerSeed(a, b).equals("B"), "incorrect strongest seed");
        require(SeedBreedingGuide.strongerSeed(a, a).equals("EVEN"), "incorrect tied seeds");
        require(SeedBreedingGuide.expected(a, b).equals(new CropGenome(30, 41, 47)), "incorrect midpoint");
        require(SeedBreedingGuide.minimum(a, b).equals(new CropGenome(25, 36, 42)), "incorrect minimum");
        require(SeedBreedingGuide.maximum(a, b).equals(new CropGenome(35, 46, 52)), "incorrect maximum");
        require(SeedBreedingGuide.weakestTrait(a, b).equals("GROWTH"), "wrong next breeding target");
        require(SeedBreedingGuide.minimum(CropGenome.wild(), new CropGenome(0, 0, 0)).growth() == 7,
                "minimum should use truncated midpoint");
        require(SeedBreedingGuide.maximum(new CropGenome(100, 100, 100),
                new CropGenome(100, 100, 100)).growth() == 100, "maximum must clamp");
    }

    private static void testOffspringBounds() {
        CropGenome[][] pairs = {
                {new CropGenome(0, 0, 0), new CropGenome(0, 0, 0)},
                {new CropGenome(100, 100, 100), new CropGenome(100, 100, 100)},
                {new CropGenome(19, 78, 39), new CropGenome(73, 35, 90)},
                {new CropGenome(25, 25, 25), new CropGenome(24, 26, 55)}
        };
        for (CropGenome[] pair : pairs) {
            CropGenome low = SeedBreedingGuide.minimum(pair[0], pair[1]);
            CropGenome high = SeedBreedingGuide.maximum(pair[0], pair[1]);
            for (long seed = 0; seed < 5000; seed++) {
                CropGenome child = GenomeBreeder.cross(pair[0], pair[1], seed);
                require(child.growth() >= low.growth() && child.growth() <= high.growth(), "growth out of range");
                require(child.yield() >= low.yield() && child.yield() <= high.yield(), "yield out of range");
                require(child.resilience() >= low.resilience() && child.resilience() <= high.resilience(),
                        "resilience out of range");
            }
        }
    }

    private static void testRecipes() {
        recipe(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, CropSpecies.VIOLET_STARFLOWER, 25);
        recipe(CropSpecies.WILD, CropSpecies.CRIMSON_CLOVER, CropSpecies.AMBER_MARIGOLD, 20);
        recipe(CropSpecies.WILD, CropSpecies.AZURE_FLAX, CropSpecies.SILVER_THISTLE, 20);
        recipe(CropSpecies.AMBER_MARIGOLD, CropSpecies.VIOLET_STARFLOWER, CropSpecies.SUNFIRE_LILY, 15);
        recipe(CropSpecies.SILVER_THISTLE, CropSpecies.VIOLET_STARFLOWER, CropSpecies.MOON_ORCHID, 15);
        require(SpeciesBreeder.mutationRecipe(CropSpecies.AZURE_FLAX, CropSpecies.AZURE_FLAX) == null,
                "same species must not mutate");
        require(SpeciesBreeder.mutationRecipe(CropSpecies.MOON_ORCHID, CropSpecies.WILD) == null,
                "unknown recipe fabricated");
    }

    private static void recipe(CropSpecies a, CropSpecies b, CropSpecies output, int percent) {
        SpeciesBreeder.MutationRecipe forward = SpeciesBreeder.mutationRecipe(a, b);
        SpeciesBreeder.MutationRecipe reverse = SpeciesBreeder.mutationRecipe(b, a);
        require(forward != null && reverse != null, "missing recipe");
        require(forward.offspring() == output && reverse.offspring() == output, "wrong mutation");
        require(forward.baseChance() == percent, "incorrect base mutation chance");
        require(forward.chanceWithSupports(5) == percent + 5, "incorrect crop support bonus");
        require(forward.chanceWithSupports(8) == percent + 8, "incorrect iron support bonus");
        require(forward.chanceWithSupports(12) == percent + 12, "incorrect obsidian support bonus");
        require(forward.chanceWithSupports(99) == percent + 12, "crop support bonus not clamped");
        for (long n = 0; n < 1000; n++) {
            require(SpeciesBreeder.cross(a, b, n) == SpeciesBreeder.cross(a, b, n),
                    "breeding became nondeterministic");
        }
    }

    private static void require(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }
}
