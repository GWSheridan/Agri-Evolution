package com.agrievolution.genetics;

import java.util.Objects;
import java.util.Random;

/** Pure, deterministic species inheritance and rare mutation rules. */
public final class SpeciesBreeder {
    public static final int STARFLOWER_MUTATION_PERCENT = 25;

    private SpeciesBreeder() {}

    public static CropSpecies cross(CropSpecies first, CropSpecies second, long seed) {
        return cross(first, second, seed, 0);
    }

    /** Breeding without a pollination bonus, as used by the Seed Breeding Station. */
    public static CropSpecies cross(CropSpecies first, CropSpecies second, long seed, int bonus) {
        return cross(first, second, seed, bonus, 0);
    }

    public static CropSpecies cross(CropSpecies first, CropSpecies second, long seed, int supportBonus, int pollinationBonus) {
        Objects.requireNonNull(first, "first parent");
        Objects.requireNonNull(second, "second parent");
        if (first == second) return first;

        Random rng = new Random(seed);
        MutationRecipe recipe = mutationRecipe(first, second);
        if (recipe != null && rng.nextInt(100) < recipe.chanceWithSupports(supportBonus) + Math.clamp(pollinationBonus, 0, 3)) {
            return recipe.offspring();
        }
        // Otherwise one parent species is inherited while Growth/Yield/Resilience blend.
        return rng.nextBoolean() ? first : second;
    }

    /** Single source of truth shared by breeding and the Seed Analyzer's predictions. */
    public static MutationRecipe mutationRecipe(CropSpecies first, CropSpecies second) {
        Objects.requireNonNull(first, "first parent");
        Objects.requireNonNull(second, "second parent");
        if (pair(first, second, CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.VIOLET_STARFLOWER, STARFLOWER_MUTATION_PERCENT);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.AMBER_MARIGOLD, 20);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.SILVER_THISTLE, 20);
        if (pair(first, second, CropSpecies.AMBER_MARIGOLD, CropSpecies.VIOLET_STARFLOWER))
            return new MutationRecipe(CropSpecies.SUNFIRE_LILY, 15);
        if (pair(first, second, CropSpecies.SILVER_THISTLE, CropSpecies.VIOLET_STARFLOWER))
            return new MutationRecipe(CropSpecies.MOON_ORCHID, 15);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.WOOD))
            return new MutationRecipe(CropSpecies.STONE, 28);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.WOOD))
            return new MutationRecipe(CropSpecies.COAL, 25);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.COPPER, 22);
        if (pair(first, second, CropSpecies.COPPER, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.IRON, 18);
        if (pair(first, second, CropSpecies.IRON, CropSpecies.COPPER))
            return new MutationRecipe(CropSpecies.REDSTONE, 18);
        if (pair(first, second, CropSpecies.IRON, CropSpecies.REDSTONE))
            return new MutationRecipe(CropSpecies.GOLD, 15);
        if (pair(first, second, CropSpecies.GOLD, CropSpecies.REDSTONE))
            return new MutationRecipe(CropSpecies.LAPIS, 13);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.STONE))
            return new MutationRecipe(CropSpecies.GRAVEL, 22);
        if (pair(first, second, CropSpecies.GRAVEL, CropSpecies.DIRT))
            return new MutationRecipe(CropSpecies.SAND, 20);
        if (pair(first, second, CropSpecies.SAND, CropSpecies.DIRT))
            return new MutationRecipe(CropSpecies.CLAY, 20);
        if (pair(first, second, CropSpecies.GRAVEL, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.FLINT, 18);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.DIRT))
            return new MutationRecipe(CropSpecies.LETTUCE, 28);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.WOOD))
            return new MutationRecipe(CropSpecies.RICE, 25);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.TOMATO, 25);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.CUCUMBER, 25);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.DIRT))
            return new MutationRecipe(CropSpecies.ONION, 25);
        if (pair(first, second, CropSpecies.ONION, CropSpecies.SILVER_THISTLE))
            return new MutationRecipe(CropSpecies.GARLIC, 22);
        if (pair(first, second, CropSpecies.TOMATO, CropSpecies.SUNFIRE_LILY))
            return new MutationRecipe(CropSpecies.PEPPER, 20);
        if (pair(first, second, CropSpecies.TOMATO, CropSpecies.MOON_ORCHID))
            return new MutationRecipe(CropSpecies.STRAWBERRY, 20);
        if (pair(first, second, CropSpecies.RICE, CropSpecies.SUNFIRE_LILY))
            return new MutationRecipe(CropSpecies.CORN, 22);
        if (pair(first, second, CropSpecies.CUCUMBER, CropSpecies.LETTUCE))
            return new MutationRecipe(CropSpecies.PEAS, 25);
        if (pair(first, second, CropSpecies.PEAS, CropSpecies.ONION))
            return new MutationRecipe(CropSpecies.BEANS, 22);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.GARLIC))
            return new MutationRecipe(CropSpecies.BROCCOLI, 22);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.ONION))
            return new MutationRecipe(CropSpecies.CABBAGE, 25);
        if (pair(first, second, CropSpecies.ONION, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.RADISH, 25);
        if (pair(first, second, CropSpecies.TOMATO, CropSpecies.PEPPER))
            return new MutationRecipe(CropSpecies.AUBERGINE, 20);
        if (pair(first, second, CropSpecies.ONION, CropSpecies.AMBER_MARIGOLD))
            return new MutationRecipe(CropSpecies.SWEET_POTATO, 22);
        if (pair(first, second, CropSpecies.TOMATO, CropSpecies.LETTUCE))
            return new MutationRecipe(CropSpecies.BASIL, 25);
        if (pair(first, second, CropSpecies.CUCUMBER, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.MINT, 25);
        if (pair(first, second, CropSpecies.BASIL, CropSpecies.WOOD))
            return new MutationRecipe(CropSpecies.ROSEMARY, 22);
        if (pair(first, second, CropSpecies.BASIL, CropSpecies.SILVER_THISTLE))
            return new MutationRecipe(CropSpecies.THYME, 22);
        if (pair(first, second, CropSpecies.BROCCOLI, CropSpecies.LETTUCE))
            return new MutationRecipe(CropSpecies.PARSLEY, 25);
        if (pair(first, second, CropSpecies.PARSLEY, CropSpecies.ONION))
            return new MutationRecipe(CropSpecies.CORIANDER, 22);
        if (pair(first, second, CropSpecies.SWEET_POTATO, CropSpecies.GARLIC))
            return new MutationRecipe(CropSpecies.GINGER, 20);
        if (pair(first, second, CropSpecies.GINGER, CropSpecies.SUNFIRE_LILY))
            return new MutationRecipe(CropSpecies.TURMERIC, 20);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.STRING, 28);
        if (pair(first, second, CropSpecies.STRING, CropSpecies.WILD))
            return new MutationRecipe(CropSpecies.WOOL, 25);
        if (pair(first, second, CropSpecies.WOOL, CropSpecies.WOOD))
            return new MutationRecipe(CropSpecies.LEATHER, 22);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.WILD))
            return new MutationRecipe(CropSpecies.BONE, 22);
        if (pair(first, second, CropSpecies.CLAY, CropSpecies.LAPIS))
            return new MutationRecipe(CropSpecies.PRISMARINE, 18);
        if (pair(first, second, CropSpecies.STRING, CropSpecies.BONE))
            return new MutationRecipe(CropSpecies.FEATHER, 28);
        if (pair(first, second, CropSpecies.COAL, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.INK_SAC, 25);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.AMBER_MARIGOLD))
            return new MutationRecipe(CropSpecies.HONEYCOMB, 22);
        if (pair(first, second, CropSpecies.CLAY, CropSpecies.HONEYCOMB))
            return new MutationRecipe(CropSpecies.SLIME_BALL, 18);
        if (pair(first, second, CropSpecies.COAL, CropSpecies.FLINT))
            return new MutationRecipe(CropSpecies.GUNPOWDER, 16);
        if (pair(first, second, CropSpecies.WOOL, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.SNOWBALL, 28);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.MOSS, 28);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.BONE))
            return new MutationRecipe(CropSpecies.CALCITE, 22);
        if (pair(first, second, CropSpecies.CLAY, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.TERRACOTTA, 22);
        if (pair(first, second, CropSpecies.STRING, CropSpecies.INK_SAC))
            return new MutationRecipe(CropSpecies.SPIDER_EYE, 18);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.STRING))
            return new MutationRecipe(CropSpecies.PAPER, 28);
        if (pair(first, second, CropSpecies.MOSS, CropSpecies.CLAY))
            return new MutationRecipe(CropSpecies.KELP, 25);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.BAMBOO, 25);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.INK_SAC))
            return new MutationRecipe(CropSpecies.BLACKSTONE, 18);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.CALCITE))
            return new MutationRecipe(CropSpecies.BASALT, 18);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.GRAVEL))
            return new MutationRecipe(CropSpecies.GRANITE, 28);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.CLAY))
            return new MutationRecipe(CropSpecies.DIORITE, 28);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.GRANITE))
            return new MutationRecipe(CropSpecies.ANDESITE, 25);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.CLAY))
            return new MutationRecipe(CropSpecies.MUD, 28);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.BASALT))
            return new MutationRecipe(CropSpecies.TUFF, 20);
        if (pair(first, second, CropSpecies.CALCITE, CropSpecies.CLAY))
            return new MutationRecipe(CropSpecies.DRIPSTONE, 20);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.BLACKSTONE))
            return new MutationRecipe(CropSpecies.DEEPSLATE, 18);
        if (pair(first, second, CropSpecies.SOUL_SAND, CropSpecies.DIRT))
            return new MutationRecipe(CropSpecies.SOUL_SOIL, 18);
        if (pair(first, second, CropSpecies.SAND, CropSpecies.GRANITE))
            return new MutationRecipe(CropSpecies.RED_SAND, 25);
        if (pair(first, second, CropSpecies.SAND, CropSpecies.STONE))
            return new MutationRecipe(CropSpecies.SANDSTONE, 28);
        if (pair(first, second, CropSpecies.SAND, CropSpecies.WILD))
            return new MutationRecipe(CropSpecies.CACTUS, 25);
        if (pair(first, second, CropSpecies.MOSS, CropSpecies.STRING))
            return new MutationRecipe(CropSpecies.VINE, 25);
        if (pair(first, second, CropSpecies.MUD, CropSpecies.WILD))
            return new MutationRecipe(CropSpecies.PACKED_MUD, 22);
        if (pair(first, second, CropSpecies.CLAY, CropSpecies.TERRACOTTA))
            return new MutationRecipe(CropSpecies.BRICK, 22);
        if (pair(first, second, CropSpecies.SNOWBALL, CropSpecies.CALCITE))
            return new MutationRecipe(CropSpecies.ICE, 20);
        if (pair(first, second, CropSpecies.ICE, CropSpecies.STONE))
            return new MutationRecipe(CropSpecies.PACKED_ICE, 18);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.CHARCOAL, 28);
        if (pair(first, second, CropSpecies.SAND, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.GLASS, 25);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.BROWN_MUSHROOM, 25);
        if (pair(first, second, CropSpecies.BROWN_MUSHROOM, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.RED_MUSHROOM, 25);
        if (pair(first, second, CropSpecies.STONE, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.MOSSY_COBBLESTONE, 22);
        if (pair(first, second, CropSpecies.VINE, CropSpecies.KELP))
            return new MutationRecipe(CropSpecies.LILY_PAD, 20);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.SNOWBALL))
            return new MutationRecipe(CropSpecies.SPRUCE_LOG, 28);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.CALCITE))
            return new MutationRecipe(CropSpecies.BIRCH_LOG, 28);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.TERRACOTTA))
            return new MutationRecipe(CropSpecies.ACACIA_LOG, 25);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.CHARCOAL))
            return new MutationRecipe(CropSpecies.DARK_OAK_LOG, 25);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.MUD))
            return new MutationRecipe(CropSpecies.MANGROVE_LOG, 22);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.STRAWBERRY))
            return new MutationRecipe(CropSpecies.CHERRY_LOG, 22);
        if (pair(first, second, CropSpecies.KELP, CropSpecies.HONEYCOMB))
            return new MutationRecipe(CropSpecies.SEA_PICKLE, 20);
        if (pair(first, second, CropSpecies.MOSS, CropSpecies.AMBER_MARIGOLD))
            return new MutationRecipe(CropSpecies.GLOW_LICHEN, 20);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.HONEYCOMB))
            return new MutationRecipe(CropSpecies.SUGAR, 28);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.BROWN_MUSHROOM))
            return new MutationRecipe(CropSpecies.COCOA_BEANS, 25);
        if (pair(first, second, CropSpecies.FEATHER, CropSpecies.CALCITE))
            return new MutationRecipe(CropSpecies.EGG, 25);
        if (pair(first, second, CropSpecies.LEATHER, CropSpecies.BROWN_MUSHROOM))
            return new MutationRecipe(CropSpecies.ROTTEN_FLESH, 25);
        if (pair(first, second, CropSpecies.LEATHER, CropSpecies.WOOL))
            return new MutationRecipe(CropSpecies.RABBIT_HIDE, 22);
        if (pair(first, second, CropSpecies.RABBIT_HIDE, CropSpecies.EMERALD))
            return new MutationRecipe(CropSpecies.RABBIT_FOOT, 18);
        if (pair(first, second, CropSpecies.LEATHER, CropSpecies.TERRACOTTA))
            return new MutationRecipe(CropSpecies.ARMADILLO_SCUTE, 20);
        if (pair(first, second, CropSpecies.SPIDER_EYE, CropSpecies.SUGAR))
            return new MutationRecipe(CropSpecies.FERMENTED_SPIDER_EYE, 20);
        if (pair(first, second, CropSpecies.CACTUS, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.GREEN_DYE, 25);
        if (pair(first, second, CropSpecies.RED_MUSHROOM, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.RED_DYE, 25);
        if (pair(first, second, CropSpecies.AMBER_MARIGOLD, CropSpecies.SUGAR))
            return new MutationRecipe(CropSpecies.YELLOW_DYE, 25);
        if (pair(first, second, CropSpecies.COCOA_BEANS, CropSpecies.CLAY))
            return new MutationRecipe(CropSpecies.BROWN_DYE, 25);
        if (pair(first, second, CropSpecies.HONEYCOMB, CropSpecies.GLASS))
            return new MutationRecipe(CropSpecies.HONEY_BOTTLE, 22);
        if (pair(first, second, CropSpecies.GLOW_LICHEN, CropSpecies.STRAWBERRY))
            return new MutationRecipe(CropSpecies.GLOW_BERRIES, 20);
        if (pair(first, second, CropSpecies.LAPIS, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.BLUE_DYE, 22);
        if (pair(first, second, CropSpecies.BLUE_DYE, CropSpecies.GREEN_DYE))
            return new MutationRecipe(CropSpecies.CYAN_DYE, 20);
        if (pair(first, second, CropSpecies.BONE, CropSpecies.SILVER_THISTLE))
            return new MutationRecipe(CropSpecies.WHITE_DYE, 25);
        if (pair(first, second, CropSpecies.INK_SAC, CropSpecies.COAL))
            return new MutationRecipe(CropSpecies.BLACK_DYE, 25);
        if (pair(first, second, CropSpecies.RED_DYE, CropSpecies.YELLOW_DYE))
            return new MutationRecipe(CropSpecies.ORANGE_DYE, 25);
        if (pair(first, second, CropSpecies.RED_DYE, CropSpecies.WHITE_DYE))
            return new MutationRecipe(CropSpecies.PINK_DYE, 25);
        if (pair(first, second, CropSpecies.RED_DYE, CropSpecies.BLUE_DYE))
            return new MutationRecipe(CropSpecies.PURPLE_DYE, 20);
        if (pair(first, second, CropSpecies.PURPLE_DYE, CropSpecies.PINK_DYE))
            return new MutationRecipe(CropSpecies.MAGENTA_DYE, 20);
        if (pair(first, second, CropSpecies.BLUE_DYE, CropSpecies.WHITE_DYE))
            return new MutationRecipe(CropSpecies.LIGHT_BLUE_DYE, 22);
        if (pair(first, second, CropSpecies.GREEN_DYE, CropSpecies.WHITE_DYE))
            return new MutationRecipe(CropSpecies.LIME_DYE, 22);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.BAMBOO))
            return new MutationRecipe(CropSpecies.STICK, 28);
        if (pair(first, second, CropSpecies.KELP, CropSpecies.CHARCOAL))
            return new MutationRecipe(CropSpecies.DRIED_KELP, 25);
        if (pair(first, second, CropSpecies.BLACK_DYE, CropSpecies.WHITE_DYE))
            return new MutationRecipe(CropSpecies.GRAY_DYE, 25);
        if (pair(first, second, CropSpecies.GRAY_DYE, CropSpecies.WHITE_DYE))
            return new MutationRecipe(CropSpecies.LIGHT_GRAY_DYE, 25);
        if (pair(first, second, CropSpecies.CUCUMBER, CropSpecies.CORN))
            return new MutationRecipe(CropSpecies.PUMPKIN, 24);
        if (pair(first, second, CropSpecies.PUMPKIN, CropSpecies.SWEET_POTATO))
            return new MutationRecipe(CropSpecies.BUTTERNUT_SQUASH, 22);
        if (pair(first, second, CropSpecies.PUMPKIN, CropSpecies.CUCUMBER))
            return new MutationRecipe(CropSpecies.COURGETTE, 24);
        if (pair(first, second, CropSpecies.RADISH, CropSpecies.CABBAGE))
            return new MutationRecipe(CropSpecies.TURNIP, 26);
        if (pair(first, second, CropSpecies.TURNIP, CropSpecies.PARSLEY))
            return new MutationRecipe(CropSpecies.PARSNIP, 24);
        if (pair(first, second, CropSpecies.ONION, CropSpecies.CELERY))
            return new MutationRecipe(CropSpecies.LEEK, 24);
        if (pair(first, second, CropSpecies.PARSLEY, CropSpecies.LETTUCE))
            return new MutationRecipe(CropSpecies.CELERY, 26);
        if (pair(first, second, CropSpecies.LEEK, CropSpecies.BROCCOLI))
            return new MutationRecipe(CropSpecies.ASPARAGUS, 22);
        if (pair(first, second, CropSpecies.STRAWBERRY, CropSpecies.AZURE_FLAX))
            return new MutationRecipe(CropSpecies.BLUEBERRY, 22);
        if (pair(first, second, CropSpecies.STRAWBERRY, CropSpecies.CRIMSON_CLOVER))
            return new MutationRecipe(CropSpecies.RASPBERRY, 24);
        if (pair(first, second, CropSpecies.RASPBERRY, CropSpecies.BLUEBERRY))
            return new MutationRecipe(CropSpecies.BLACKBERRY, 20);
        if (pair(first, second, CropSpecies.BLUEBERRY, CropSpecies.RICE))
            return new MutationRecipe(CropSpecies.CRANBERRY, 22);
        if (pair(first, second, CropSpecies.LETTUCE, CropSpecies.CELERY))
            return new MutationRecipe(CropSpecies.SPINACH, 26);
        if (pair(first, second, CropSpecies.BROCCOLI, CropSpecies.CABBAGE))
            return new MutationRecipe(CropSpecies.CAULIFLOWER, 24);
        if (pair(first, second, CropSpecies.ASPARAGUS, CropSpecies.CAULIFLOWER))
            return new MutationRecipe(CropSpecies.ARTICHOKE, 22);
        if (pair(first, second, CropSpecies.PEPPER, CropSpecies.GINGER))
            return new MutationRecipe(CropSpecies.CHILLI, 24);
        if (pair(first, second, CropSpecies.ONION, CropSpecies.LEEK))
            return new MutationRecipe(CropSpecies.SPRING_ONION, 26);
        if (pair(first, second, CropSpecies.CELERY, CropSpecies.PARSNIP))
            return new MutationRecipe(CropSpecies.FENNEL, 24);
        if (pair(first, second, CropSpecies.PARSLEY, CropSpecies.FENNEL))
            return new MutationRecipe(CropSpecies.DILL, 26);
        if (pair(first, second, CropSpecies.BEANS, CropSpecies.SWEET_POTATO))
            return new MutationRecipe(CropSpecies.PEANUT, 24);
        if (pair(first, second, CropSpecies.PEANUT, CropSpecies.RICE))
            return new MutationRecipe(CropSpecies.SESAME, 22);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.RICE))
            return new MutationRecipe(CropSpecies.OATS, 26);
        if (pair(first, second, CropSpecies.CRANBERRY, CropSpecies.CUCUMBER))
            return new MutationRecipe(CropSpecies.GOOSEBERRY, 22);
        if (pair(first, second, CropSpecies.BLACKBERRY, CropSpecies.BLUEBERRY))
            return new MutationRecipe(CropSpecies.BLACKCURRANT, 22);
        if (pair(first, second, CropSpecies.SPINACH, CropSpecies.CABBAGE))
            return new MutationRecipe(CropSpecies.KALE, 26);
        if (pair(first, second, CropSpecies.KALE, CropSpecies.LETTUCE))
            return new MutationRecipe(CropSpecies.PAK_CHOI, 24);
        if (pair(first, second, CropSpecies.CABBAGE, CropSpecies.CAULIFLOWER))
            return new MutationRecipe(CropSpecies.BRUSSELS_SPROUTS, 22);
        if (pair(first, second, CropSpecies.TURNIP, CropSpecies.SWEET_POTATO))
            return new MutationRecipe(CropSpecies.SWEDE, 24);
        if (pair(first, second, CropSpecies.CELERY, CropSpecies.TURNIP))
            return new MutationRecipe(CropSpecies.CELERIAC, 24);
        if (pair(first, second, CropSpecies.PEAS, CropSpecies.SESAME))
            return new MutationRecipe(CropSpecies.LENTIL, 24);
        if (pair(first, second, CropSpecies.BEANS, CropSpecies.PEANUT))
            return new MutationRecipe(CropSpecies.CHICKPEA, 24);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.OATS))
            return new MutationRecipe(CropSpecies.BARLEY, 26);
        if (pair(first, second, CropSpecies.BARLEY, CropSpecies.AMBER_MARIGOLD))
            return new MutationRecipe(CropSpecies.BUCKWHEAT, 22);
        if (pair(first, second, CropSpecies.CELERY, CropSpecies.CRANBERRY))
            return new MutationRecipe(CropSpecies.RHUBARB, 22);
        if (pair(first, second, CropSpecies.BLACKCURRANT, CropSpecies.CUCUMBER))
            return new MutationRecipe(CropSpecies.GRAPE, 22);
        if (pair(first, second, CropSpecies.PUMPKIN, CropSpecies.GOOSEBERRY))
            return new MutationRecipe(CropSpecies.CANTALOUPE, 22);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.VINE))
            return new MutationRecipe(CropSpecies.JUNGLE_LOG, 28);
        if (pair(first, second, CropSpecies.GRAVEL, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.COARSE_DIRT, 28);
        if (pair(first, second, CropSpecies.DIRT, CropSpecies.VINE))
            return new MutationRecipe(CropSpecies.ROOTED_DIRT, 26);
        if (pair(first, second, CropSpecies.RED_SAND, CropSpecies.STONE))
            return new MutationRecipe(CropSpecies.RED_SANDSTONE, 25);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.STONE))
            return new MutationRecipe(CropSpecies.PRISMARINE_BRICKS, 18);
        if (pair(first, second, CropSpecies.NETHER_WART, CropSpecies.WARPED_FUNGUS))
            return new MutationRecipe(CropSpecies.WARPED_WART_BLOCK, 16);
        if (pair(first, second, CropSpecies.NETHER_BRICK, CropSpecies.NETHER_WART))
            return new MutationRecipe(CropSpecies.RED_NETHER_BRICKS, 18);
        if (pair(first, second, CropSpecies.WILD, CropSpecies.MOSS))
            return new MutationRecipe(CropSpecies.GRASS_BLOCK, 28);
        if (pair(first, second, CropSpecies.GRASS_BLOCK, CropSpecies.SPRUCE_LOG))
            return new MutationRecipe(CropSpecies.PODZOL, 26);
        if (pair(first, second, CropSpecies.BROWN_MUSHROOM, CropSpecies.GRASS_BLOCK))
            return new MutationRecipe(CropSpecies.MYCELIUM, 24);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.RED_SAND))
            return new MutationRecipe(CropSpecies.DEAD_BUSH, 28);
        if (pair(first, second, CropSpecies.PACKED_MUD, CropSpecies.BRICK))
            return new MutationRecipe(CropSpecies.MUD_BRICKS, 22);
        if (pair(first, second, CropSpecies.DEEPSLATE, CropSpecies.BRICK))
            return new MutationRecipe(CropSpecies.DEEPSLATE_BRICKS, 20);
        if (pair(first, second, CropSpecies.SUGAR, CropSpecies.PAPER))
            return new MutationRecipe(CropSpecies.SUGAR_CANE, 28);
        if (pair(first, second, CropSpecies.STRAWBERRY, CropSpecies.VINE))
            return new MutationRecipe(CropSpecies.SWEET_BERRIES, 26);
        if (pair(first, second, CropSpecies.MOSS, CropSpecies.KELP))
            return new MutationRecipe(CropSpecies.SEAGRASS, 26);
        if (pair(first, second, CropSpecies.ROOTED_DIRT, CropSpecies.VINE))
            return new MutationRecipe(CropSpecies.HANGING_ROOTS, 26);
        if (pair(first, second, CropSpecies.AMBER_MARIGOLD, CropSpecies.SUNFIRE_LILY))
            return new MutationRecipe(CropSpecies.TORCHFLOWER, 18);
        if (pair(first, second, CropSpecies.CACTUS, CropSpecies.LILY_PAD))
            return new MutationRecipe(CropSpecies.PITCHER_PLANT, 18);
        if (pair(first, second, CropSpecies.MYCELIUM, CropSpecies.VIOLET_STARFLOWER))
            return new MutationRecipe(CropSpecies.SPORE_BLOSSOM, 16);
        if (pair(first, second, CropSpecies.LILY_PAD, CropSpecies.JUNGLE_LOG))
            return new MutationRecipe(CropSpecies.BIG_DRIPLEAF, 20);
        if (pair(first, second, CropSpecies.WOOD, CropSpecies.HANGING_ROOTS))
            return new MutationRecipe(CropSpecies.OAK_SAPLING, 28);
        if (pair(first, second, CropSpecies.SPRUCE_LOG, CropSpecies.HANGING_ROOTS))
            return new MutationRecipe(CropSpecies.SPRUCE_SAPLING, 26);
        if (pair(first, second, CropSpecies.BIRCH_LOG, CropSpecies.HANGING_ROOTS))
            return new MutationRecipe(CropSpecies.BIRCH_SAPLING, 26);
        if (pair(first, second, CropSpecies.DARK_OAK_LOG, CropSpecies.HANGING_ROOTS))
            return new MutationRecipe(CropSpecies.DARK_OAK_SAPLING, 24);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.BLUE_DYE))
            return new MutationRecipe(CropSpecies.TUBE_CORAL, 18);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.PINK_DYE))
            return new MutationRecipe(CropSpecies.BRAIN_CORAL, 18);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.PURPLE_DYE))
            return new MutationRecipe(CropSpecies.BUBBLE_CORAL, 18);
        if (pair(first, second, CropSpecies.PRISMARINE, CropSpecies.RED_DYE))
            return new MutationRecipe(CropSpecies.FIRE_CORAL, 18);
        return null;
    }

    public record MutationRecipe(CropSpecies offspring, int baseChance) {
        public int chanceWithSupports(int bonus) {
            return baseChance + Math.clamp(bonus, 0, 12);
        }
    }

    private static boolean pair(CropSpecies first, CropSpecies second,
                                CropSpecies a, CropSpecies b) {
        return (first == a && second == b) || (first == b && second == a);
    }
}
