package com.agrievolution.genetics;

import java.util.Objects;
import java.util.Random;

/** Tier-3 laboratory-only hybridisation: existing field-breeding recipes are unchanged. */
public final class AdvancedHybridBreeder {
    private AdvancedHybridBreeder() { }

    public record HybridRecipe(CropSpecies offspring, int baseChance) {
        public int chance(boolean support, boolean greenhouse) {
            return chance(support?8:0,greenhouse);
        }
        public int chance(int supportBonus, boolean greenhouse) {
            return Math.min(60, baseChance + supportBonus + (greenhouse ? 5 : 0));
        }
    }

    public static HybridRecipe recipe(CropSpecies first, CropSpecies second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        if (pair(first, second, CropSpecies.SUNFIRE_LILY, CropSpecies.MOON_ORCHID))
            return new HybridRecipe(CropSpecies.ECLIPSE_LOTUS, 12);
        if (pair(first, second, CropSpecies.AMBER_MARIGOLD, CropSpecies.SILVER_THISTLE))
            return new HybridRecipe(CropSpecies.TWILIGHT_SAGE, 16);
        if (pair(first, second, CropSpecies.GOLD, CropSpecies.LAPIS))
            return new HybridRecipe(CropSpecies.DIAMOND, 10);
        if (pair(first, second, CropSpecies.DIAMOND, CropSpecies.LAPIS))
            return new HybridRecipe(CropSpecies.EMERALD, 8);
        if (pair(first, second, CropSpecies.COAL, CropSpecies.LAPIS))
            return new HybridRecipe(CropSpecies.QUARTZ, 12);
        if (pair(first, second, CropSpecies.REDSTONE, CropSpecies.QUARTZ))
            return new HybridRecipe(CropSpecies.GLOWSTONE, 12);
        if (pair(first, second, CropSpecies.DIAMOND, CropSpecies.QUARTZ))
            return new HybridRecipe(CropSpecies.OBSIDIAN, 8);
        if (pair(first, second, CropSpecies.QUARTZ, CropSpecies.OBSIDIAN))
            return new HybridRecipe(CropSpecies.NETHERRACK, 14);
        if (pair(first, second, CropSpecies.NETHERRACK, CropSpecies.SAND))
            return new HybridRecipe(CropSpecies.SOUL_SAND, 16);
        if (pair(first, second, CropSpecies.NETHERRACK, CropSpecies.GLOWSTONE))
            return new HybridRecipe(CropSpecies.MAGMA_CREAM, 12);
        if (pair(first, second, CropSpecies.SOUL_SAND, CropSpecies.CRIMSON_CLOVER))
            return new HybridRecipe(CropSpecies.NETHER_WART, 14);
        if (pair(first, second, CropSpecies.MAGMA_CREAM, CropSpecies.NETHER_WART))
            return new HybridRecipe(CropSpecies.BLAZE_ROD, 10);
        if (pair(first, second, CropSpecies.OBSIDIAN, CropSpecies.EMERALD))
            return new HybridRecipe(CropSpecies.END_STONE, 12);
        if (pair(first, second, CropSpecies.END_STONE, CropSpecies.AZURE_FLAX))
            return new HybridRecipe(CropSpecies.CHORUS_FRUIT, 12);
        if (pair(first, second, CropSpecies.END_STONE, CropSpecies.CHORUS_FRUIT))
            return new HybridRecipe(CropSpecies.ENDER_PEARL, 8);
        if (pair(first, second, CropSpecies.ENDER_PEARL, CropSpecies.CHORUS_FRUIT))
            return new HybridRecipe(CropSpecies.SHULKER_SHELL, 6);
        if (pair(first, second, CropSpecies.BLAZE_ROD, CropSpecies.OBSIDIAN))
            return new HybridRecipe(CropSpecies.ANCIENT_DEBRIS, 7);
        if (pair(first, second, CropSpecies.ANCIENT_DEBRIS, CropSpecies.ENDER_PEARL))
            return new HybridRecipe(CropSpecies.NETHERITE, 4);
        if (pair(first, second, CropSpecies.QUARTZ, CropSpecies.LAPIS))
            return new HybridRecipe(CropSpecies.AMETHYST, 14);
        if (pair(first, second, CropSpecies.SOUL_SAND, CropSpecies.ENDER_PEARL))
            return new HybridRecipe(CropSpecies.GHAST_TEAR, 8);
        if (pair(first, second, CropSpecies.BLAZE_ROD, CropSpecies.AMETHYST))
            return new HybridRecipe(CropSpecies.BREEZE_ROD, 8);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.GLOWSTONE))
            return new HybridRecipe(CropSpecies.PRISMARINE_CRYSTALS, 12);
        if (pair(first, second, CropSpecies.GHAST_TEAR, CropSpecies.FEATHER))
            return new HybridRecipe(CropSpecies.PHANTOM_MEMBRANE, 8);
        if (pair(first, second, CropSpecies.AMETHYST, CropSpecies.SHULKER_SHELL))
            return new HybridRecipe(CropSpecies.ECHO_SHARD, 5);
        if (pair(first, second, CropSpecies.INK_SAC, CropSpecies.GLOWSTONE))
            return new HybridRecipe(CropSpecies.GLOW_INK_SAC, 14);
        if (pair(first, second, CropSpecies.OBSIDIAN, CropSpecies.GHAST_TEAR))
            return new HybridRecipe(CropSpecies.CRYING_OBSIDIAN, 8);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.SHULKER_SHELL))
            return new HybridRecipe(CropSpecies.NAUTILUS_SHELL, 6);
        if (pair(first, second, CropSpecies.CHORUS_FRUIT, CropSpecies.AMETHYST))
            return new HybridRecipe(CropSpecies.PURPUR, 12);
        if (pair(first, second, CropSpecies.KELP, CropSpecies.PRISMARINE))
            return new HybridRecipe(CropSpecies.TURTLE_SCUTE, 10);
        if (pair(first, second, CropSpecies.NAUTILUS_SHELL, CropSpecies.SLIME_BALL))
            return new HybridRecipe(CropSpecies.SPONGE, 6);
        if (pair(first, second, CropSpecies.PRISMARINE_CRYSTALS, CropSpecies.GLOW_INK_SAC))
            return new HybridRecipe(CropSpecies.SEA_LANTERN, 12);
        if (pair(first, second, CropSpecies.NETHER_WART, CropSpecies.GLOWSTONE))
            return new HybridRecipe(CropSpecies.SHROOMLIGHT, 12);
        if (pair(first, second, CropSpecies.CHORUS_FRUIT, CropSpecies.BLAZE_ROD))
            return new HybridRecipe(CropSpecies.END_ROD, 8);
        if (pair(first, second, CropSpecies.ECHO_SHARD, CropSpecies.SOUL_SOIL))
            return new HybridRecipe(CropSpecies.SCULK, 6);
        if (pair(first, second, CropSpecies.PACKED_ICE, CropSpecies.DIAMOND))
            return new HybridRecipe(CropSpecies.BLUE_ICE, 10);
        if (pair(first, second, CropSpecies.NETHERRACK, CropSpecies.COAL))
            return new HybridRecipe(CropSpecies.NETHER_BRICK, 12);
        if (pair(first, second, CropSpecies.ENDER_PEARL, CropSpecies.BLAZE_ROD))
            return new HybridRecipe(CropSpecies.ENDER_EYE, 8);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.INK_SAC))
            return new HybridRecipe(CropSpecies.DARK_PRISMARINE, 14);
        if (pair(first, second, CropSpecies.BLACKSTONE, CropSpecies.GOLD))
            return new HybridRecipe(CropSpecies.GILDED_BLACKSTONE, 12);
        if (pair(first, second, CropSpecies.SCULK, CropSpecies.BONE))
            return new HybridRecipe(CropSpecies.SCULK_CATALYST, 6);
        if (pair(first, second, CropSpecies.SHROOMLIGHT, CropSpecies.MAGMA_CREAM))
            return new HybridRecipe(CropSpecies.OCHRE_FROGLIGHT, 8);
        if (pair(first, second, CropSpecies.NETHERRACK, CropSpecies.RED_MUSHROOM))
            return new HybridRecipe(CropSpecies.CRIMSON_STEM, 12);
        if (pair(first, second, CropSpecies.ENDER_PEARL, CropSpecies.BROWN_MUSHROOM))
            return new HybridRecipe(CropSpecies.WARPED_STEM, 12);
        if (pair(first, second, CropSpecies.OCHRE_FROGLIGHT, CropSpecies.SLIME_BALL))
            return new HybridRecipe(CropSpecies.VERDANT_FROGLIGHT, 8);
        if (pair(first, second, CropSpecies.OCHRE_FROGLIGHT, CropSpecies.AMETHYST))
            return new HybridRecipe(CropSpecies.PEARLESCENT_FROGLIGHT, 8);
        if (pair(first, second, CropSpecies.SOUL_SAND, CropSpecies.BLACKSTONE))
            return new HybridRecipe(CropSpecies.WITHER_ROSE, 10);
        if (pair(first, second, CropSpecies.CHORUS_FRUIT, CropSpecies.PURPUR))
            return new HybridRecipe(CropSpecies.CHORUS_FLOWER, 10);
        if (pair(first, second, CropSpecies.CRIMSON_STEM, CropSpecies.NETHER_WART))
            return new HybridRecipe(CropSpecies.CRIMSON_FUNGUS, 12);
        if (pair(first, second, CropSpecies.WARPED_STEM, CropSpecies.ENDER_PEARL))
            return new HybridRecipe(CropSpecies.WARPED_FUNGUS, 10);
        if (pair(first, second, CropSpecies.BLAZE_ROD, CropSpecies.GUNPOWDER))
            return new HybridRecipe(CropSpecies.FIRE_CHARGE, 10);
        if (pair(first, second, CropSpecies.BREEZE_ROD, CropSpecies.PRISMARINE_CRYSTALS))
            return new HybridRecipe(CropSpecies.WIND_CHARGE, 8);
        if (pair(first, second, CropSpecies.GHAST_TEAR, CropSpecies.CHORUS_FLOWER))
            return new HybridRecipe(CropSpecies.DRAGON_BREATH, 6);
        if (pair(first, second, CropSpecies.ECHO_SHARD, CropSpecies.EMERALD))
            return new HybridRecipe(CropSpecies.EXPERIENCE_BOTTLE, 6);
        if (pair(first, second, CropSpecies.NAUTILUS_SHELL, CropSpecies.PRISMARINE_CRYSTALS))
            return new HybridRecipe(CropSpecies.HEART_OF_THE_SEA, 6);
        if (pair(first, second, CropSpecies.WITHER_ROSE, CropSpecies.NETHERITE))
            return new HybridRecipe(CropSpecies.NETHER_STAR, 4);
        if (pair(first, second, CropSpecies.DRAGON_BREATH, CropSpecies.CHORUS_FLOWER))
            return new HybridRecipe(CropSpecies.DRAGON_HEAD, 5);
        if (pair(first, second, CropSpecies.WIND_CHARGE, CropSpecies.ANCIENT_DEBRIS))
            return new HybridRecipe(CropSpecies.HEAVY_CORE, 4);
        if (pair(first, second, CropSpecies.BONE, CropSpecies.ECHO_SHARD))
            return new HybridRecipe(CropSpecies.SKELETON_SKULL, 6);
        if (pair(first, second, CropSpecies.WITHER_ROSE, CropSpecies.BONE))
            return new HybridRecipe(CropSpecies.WITHER_SKELETON_SKULL, 4);
        if (pair(first, second, CropSpecies.GUNPOWDER, CropSpecies.ROTTEN_FLESH))
            return new HybridRecipe(CropSpecies.CREEPER_HEAD, 6);
        if (pair(first, second, CropSpecies.ROTTEN_FLESH, CropSpecies.ECHO_SHARD))
            return new HybridRecipe(CropSpecies.ZOMBIE_HEAD, 6);
        if (pair(first, second, CropSpecies.SCULK, CropSpecies.AMETHYST))
            return new HybridRecipe(CropSpecies.SCULK_SENSOR, 6);
        if (pair(first, second, CropSpecies.SCULK_CATALYST, CropSpecies.ECHO_SHARD))
            return new HybridRecipe(CropSpecies.SCULK_SHRIEKER, 4);
        if (pair(first, second, CropSpecies.HEART_OF_THE_SEA, CropSpecies.NAUTILUS_SHELL))
            return new HybridRecipe(CropSpecies.CONDUIT, 4);
        if (pair(first, second, CropSpecies.CRYING_OBSIDIAN, CropSpecies.GLOWSTONE))
            return new HybridRecipe(CropSpecies.RESPAWN_ANCHOR, 5);
        if (pair(first, second, CropSpecies.SCULK_SENSOR, CropSpecies.AMETHYST))
            return new HybridRecipe(CropSpecies.CALIBRATED_SCULK_SENSOR, 5);
        if (pair(first, second, CropSpecies.GHAST_TEAR, CropSpecies.ENDER_EYE))
            return new HybridRecipe(CropSpecies.END_CRYSTAL, 4);
        if (pair(first, second, CropSpecies.NETHER_STAR, CropSpecies.EMERALD))
            return new HybridRecipe(CropSpecies.TOTEM_OF_UNDYING, 2);
        if (pair(first, second, CropSpecies.GOLD, CropSpecies.NETHER_STAR))
            return new HybridRecipe(CropSpecies.ENCHANTED_GOLDEN_APPLE, 2);
        if (pair(first, second, CropSpecies.BREEZE_ROD, CropSpecies.COPPER))
            return new HybridRecipe(CropSpecies.TRIAL_KEY, 5);
        if (pair(first, second, CropSpecies.TRIAL_KEY, CropSpecies.ECHO_SHARD))
            return new HybridRecipe(CropSpecies.OMINOUS_TRIAL_KEY, 3);
        if (pair(first, second, CropSpecies.ANCIENT_DEBRIS, CropSpecies.PAPER))
            return new HybridRecipe(CropSpecies.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 3);
        if (pair(first, second, CropSpecies.ECHO_SHARD, CropSpecies.BLACK_DYE))
            return new HybridRecipe(CropSpecies.DISC_FRAGMENT_5, 5);
        if (pair(first, second, CropSpecies.NAUTILUS_SHELL, CropSpecies.PAPER))
            return new HybridRecipe(CropSpecies.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 5);
        if (pair(first, second, CropSpecies.HEART_OF_THE_SEA, CropSpecies.PAPER))
            return new HybridRecipe(CropSpecies.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 4);
        if (pair(first, second, CropSpecies.GILDED_BLACKSTONE, CropSpecies.PAPER))
            return new HybridRecipe(CropSpecies.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 4);
        if (pair(first, second, CropSpecies.SCULK_SHRIEKER, CropSpecies.PAPER))
            return new HybridRecipe(CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 2);
        return null;
    }

    public static CropSpecies crossSpecies(CropSpecies a, CropSpecies b,
                                           long seed, boolean support, boolean greenhouse) {
        return crossSpecies(a,b,seed,support?8:0,greenhouse);
    }
    public static CropSpecies crossSpecies(CropSpecies a,CropSpecies b,long seed,int supportBonus,boolean greenhouse) {
        if (a == b) throw new IllegalArgumentException("Hybridisation requires two different species");
        Random random = new Random(seed);
        HybridRecipe recipe = recipe(a, b);
        if (recipe != null && random.nextInt(100) < recipe.chance(supportBonus, greenhouse))
            return recipe.offspring();
        return random.nextBoolean() ? a : b;
    }

    /** Average inherited genes, with modest tier-3 laboratory refinement. */
    public static CropGenome crossGenes(CropGenome a, CropGenome b, long seed, boolean support) {
        CropGenome inherited = GenomeBreeder.cross(a, b, seed);
        int stabilisation = support ? 4 : 2;
        return new CropGenome(inherited.growth() + stabilisation,
                inherited.yield() + stabilisation, inherited.resilience() + stabilisation);
    }

    private static boolean pair(CropSpecies a, CropSpecies b, CropSpecies x, CropSpecies y) {
        return (a == x && b == y) || (a == y && b == x);
    }
}
