package com.agrievolution.genetics;

/** JDK-only mutation, species ID, and mixed-cross deterministic checks. */
public final class SpeciesSmokeTest {
    public static void main(String[] args) {
        verifyRecipe(CropSpecies.SPINACH,CropSpecies.CABBAGE,CropSpecies.KALE,26);
        verifyRecipe(CropSpecies.KALE,CropSpecies.LETTUCE,CropSpecies.PAK_CHOI,24);
        verifyRecipe(CropSpecies.CABBAGE,CropSpecies.CAULIFLOWER,CropSpecies.BRUSSELS_SPROUTS,22);
        verifyRecipe(CropSpecies.TURNIP,CropSpecies.SWEET_POTATO,CropSpecies.SWEDE,24);
        verifyRecipe(CropSpecies.CELERY,CropSpecies.TURNIP,CropSpecies.CELERIAC,24);
        verifyRecipe(CropSpecies.PEAS,CropSpecies.SESAME,CropSpecies.LENTIL,24);
        verifyRecipe(CropSpecies.BEANS,CropSpecies.PEANUT,CropSpecies.CHICKPEA,24);
        verifyRecipe(CropSpecies.WILD,CropSpecies.OATS,CropSpecies.BARLEY,26);
        verifyRecipe(CropSpecies.BARLEY,CropSpecies.AMBER_MARIGOLD,CropSpecies.BUCKWHEAT,22);
        verifyRecipe(CropSpecies.CELERY,CropSpecies.CRANBERRY,CropSpecies.RHUBARB,22);
        verifyRecipe(CropSpecies.BLACKCURRANT,CropSpecies.CUCUMBER,CropSpecies.GRAPE,22);
        verifyRecipe(CropSpecies.PUMPKIN,CropSpecies.GOOSEBERRY,CropSpecies.CANTALOUPE,22);
        verifyRecipe(CropSpecies.LETTUCE,CropSpecies.CELERY,CropSpecies.SPINACH,26);
        verifyRecipe(CropSpecies.BROCCOLI,CropSpecies.CABBAGE,CropSpecies.CAULIFLOWER,24);
        verifyRecipe(CropSpecies.ASPARAGUS,CropSpecies.CAULIFLOWER,CropSpecies.ARTICHOKE,22);
        verifyRecipe(CropSpecies.PEPPER,CropSpecies.GINGER,CropSpecies.CHILLI,24);
        verifyRecipe(CropSpecies.ONION,CropSpecies.LEEK,CropSpecies.SPRING_ONION,26);
        verifyRecipe(CropSpecies.CELERY,CropSpecies.PARSNIP,CropSpecies.FENNEL,24);
        verifyRecipe(CropSpecies.PARSLEY,CropSpecies.FENNEL,CropSpecies.DILL,26);
        verifyRecipe(CropSpecies.BEANS,CropSpecies.SWEET_POTATO,CropSpecies.PEANUT,24);
        verifyRecipe(CropSpecies.PEANUT,CropSpecies.RICE,CropSpecies.SESAME,22);
        verifyRecipe(CropSpecies.WILD,CropSpecies.RICE,CropSpecies.OATS,26);
        verifyRecipe(CropSpecies.CRANBERRY,CropSpecies.CUCUMBER,CropSpecies.GOOSEBERRY,22);
        verifyRecipe(CropSpecies.BLACKBERRY,CropSpecies.BLUEBERRY,CropSpecies.BLACKCURRANT,22);
        verifyRecipe(CropSpecies.CUCUMBER,CropSpecies.CORN,CropSpecies.PUMPKIN,24);
        verifyRecipe(CropSpecies.PUMPKIN,CropSpecies.SWEET_POTATO,CropSpecies.BUTTERNUT_SQUASH,22);
        verifyRecipe(CropSpecies.PUMPKIN,CropSpecies.CUCUMBER,CropSpecies.COURGETTE,24);
        verifyRecipe(CropSpecies.RADISH,CropSpecies.CABBAGE,CropSpecies.TURNIP,26);
        verifyRecipe(CropSpecies.TURNIP,CropSpecies.PARSLEY,CropSpecies.PARSNIP,24);
        verifyRecipe(CropSpecies.ONION,CropSpecies.CELERY,CropSpecies.LEEK,24);
        verifyRecipe(CropSpecies.PARSLEY,CropSpecies.LETTUCE,CropSpecies.CELERY,26);
        verifyRecipe(CropSpecies.LEEK,CropSpecies.BROCCOLI,CropSpecies.ASPARAGUS,22);
        verifyRecipe(CropSpecies.STRAWBERRY,CropSpecies.AZURE_FLAX,CropSpecies.BLUEBERRY,22);
        verifyRecipe(CropSpecies.STRAWBERRY,CropSpecies.CRIMSON_CLOVER,CropSpecies.RASPBERRY,24);
        verifyRecipe(CropSpecies.RASPBERRY,CropSpecies.BLUEBERRY,CropSpecies.BLACKBERRY,20);
        verifyRecipe(CropSpecies.BLUEBERRY,CropSpecies.RICE,CropSpecies.CRANBERRY,22);
        if (CropSpecies.values().length != 265) throw new AssertionError("unexpected species count");
        if (CropSpecies.NETHERITE.ordinal()!=38 || CropSpecies.RICE.ordinal()!=46 || CropSpecies.SWEET_POTATO.ordinal()!=54)
            throw new AssertionError("existing saved species ordinals changed");
        for (CropSpecies species : CropSpecies.values()) {
            if (species.id().isBlank() || species.seedId().isBlank() || species.blockId().isBlank())
                throw new AssertionError("missing persistent species ID");
            if (SpeciesBreeder.cross(species, species, 42L) != species)
                throw new AssertionError("same-species cross changed species: " + species);
        }
        int mutations = 0;
        for (long seed = 0L; seed < 10000L; seed++) {
            CropSpecies child = SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed);
            if (child != SpeciesBreeder.cross(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX, seed))
                throw new AssertionError("cross must be deterministic");
            if (child == CropSpecies.VIOLET_STARFLOWER) mutations++;
            else if (child != CropSpecies.CRIMSON_CLOVER && child != CropSpecies.AZURE_FLAX)
                throw new AssertionError("unexpected cross result");
            CropSpecies reverse = SpeciesBreeder.cross(CropSpecies.AZURE_FLAX, CropSpecies.CRIMSON_CLOVER, seed);
            if ((reverse == CropSpecies.VIOLET_STARFLOWER) != (child == CropSpecies.VIOLET_STARFLOWER))
                throw new AssertionError("mutation should be symmetrical for parent order");
            CropSpecies wildCross = SpeciesBreeder.cross(CropSpecies.WILD, CropSpecies.CRIMSON_CLOVER, seed);
            if (wildCross == CropSpecies.VIOLET_STARFLOWER)
                throw new AssertionError("unrelated species caused mutation");
        }
        if (mutations < 2100 || mutations > 2900)
            throw new AssertionError("rare mutation rate looks wrong: " + mutations);
        verifyRecipe(CropSpecies.WILD, CropSpecies.CRIMSON_CLOVER, CropSpecies.AMBER_MARIGOLD, 20);
        verifyRecipe(CropSpecies.WILD, CropSpecies.AZURE_FLAX, CropSpecies.SILVER_THISTLE, 20);
        verifyRecipe(CropSpecies.AMBER_MARIGOLD, CropSpecies.VIOLET_STARFLOWER, CropSpecies.SUNFIRE_LILY, 15);
        verifyRecipe(CropSpecies.SILVER_THISTLE, CropSpecies.VIOLET_STARFLOWER, CropSpecies.MOON_ORCHID, 15);
        verifyRecipe(CropSpecies.WILD, CropSpecies.DIRT, CropSpecies.LETTUCE, 28);
        verifyRecipe(CropSpecies.WILD, CropSpecies.WOOD, CropSpecies.RICE, 25);
        verifyRecipe(CropSpecies.LETTUCE, CropSpecies.CRIMSON_CLOVER, CropSpecies.TOMATO, 25);
        verifyRecipe(CropSpecies.LETTUCE, CropSpecies.AZURE_FLAX, CropSpecies.CUCUMBER, 25);
        verifyRecipe(CropSpecies.LETTUCE, CropSpecies.DIRT, CropSpecies.ONION, 25);
        verifyRecipe(CropSpecies.ONION, CropSpecies.SILVER_THISTLE, CropSpecies.GARLIC, 22);
        verifyRecipe(CropSpecies.TOMATO, CropSpecies.SUNFIRE_LILY, CropSpecies.PEPPER, 20);
        verifyRecipe(CropSpecies.TOMATO, CropSpecies.MOON_ORCHID, CropSpecies.STRAWBERRY, 20);
        verifyRecipe(CropSpecies.RICE, CropSpecies.SUNFIRE_LILY, CropSpecies.CORN, 22);
        verifyRecipe(CropSpecies.CUCUMBER, CropSpecies.LETTUCE, CropSpecies.PEAS, 25);
        verifyRecipe(CropSpecies.PEAS, CropSpecies.ONION, CropSpecies.BEANS, 22);
        verifyRecipe(CropSpecies.LETTUCE, CropSpecies.GARLIC, CropSpecies.BROCCOLI, 22);
        verifyRecipe(CropSpecies.LETTUCE, CropSpecies.ONION, CropSpecies.CABBAGE, 25);
        verifyRecipe(CropSpecies.ONION, CropSpecies.CRIMSON_CLOVER, CropSpecies.RADISH, 25);
        verifyRecipe(CropSpecies.TOMATO, CropSpecies.PEPPER, CropSpecies.AUBERGINE, 20);
        verifyRecipe(CropSpecies.ONION, CropSpecies.AMBER_MARIGOLD, CropSpecies.SWEET_POTATO, 22);
        verifyRecipe(CropSpecies.TOMATO, CropSpecies.LETTUCE, CropSpecies.BASIL, 25);
        verifyRecipe(CropSpecies.CUCUMBER, CropSpecies.AZURE_FLAX, CropSpecies.MINT, 25);
        verifyRecipe(CropSpecies.BASIL, CropSpecies.WOOD, CropSpecies.ROSEMARY, 22);
        verifyRecipe(CropSpecies.BASIL, CropSpecies.SILVER_THISTLE, CropSpecies.THYME, 22);
        verifyRecipe(CropSpecies.BROCCOLI, CropSpecies.LETTUCE, CropSpecies.PARSLEY, 25);
        verifyRecipe(CropSpecies.PARSLEY, CropSpecies.ONION, CropSpecies.CORIANDER, 22);
        verifyRecipe(CropSpecies.SWEET_POTATO, CropSpecies.GARLIC, CropSpecies.GINGER, 20);
        verifyRecipe(CropSpecies.GINGER, CropSpecies.SUNFIRE_LILY, CropSpecies.TURMERIC, 20);
        System.out.println("Agri Evolution species self-test PASSED: " + mutations
                + "/10000 crossbreeds mutated into Violet Starflower");
    }
    private static void verifyRecipe(CropSpecies a, CropSpecies b, CropSpecies mutation, int chance) {
        int total = 0;
        for (long s = 0; s < 10000; s++) {
            CropSpecies child = SpeciesBreeder.cross(a, b, s);
            CropSpecies reverse = SpeciesBreeder.cross(b, a, s);
            if (child != reverse && (child == mutation || reverse == mutation)) {
                throw new AssertionError("Recipe depends on parent order: " + mutation);
            }
            if (child == mutation) total++;
            else if (child != a && child != b) throw new AssertionError("Wrong recipe result: " + child);
            if (child != SpeciesBreeder.cross(a, b, s)) throw new AssertionError("Non-deterministic cross");
        }
        if (total < (chance - 3) * 100 || total > (chance + 3) * 100) {
            throw new AssertionError("Unexpected mutation rate " + mutation + ": " + total);
        }
        System.out.println(mutation + ": " + total + "/10000 crosses");
    }

}

