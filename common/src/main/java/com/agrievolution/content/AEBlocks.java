package com.agrievolution.content;

import com.agrievolution.platform.RegistrationSet;
import java.util.function.Supplier;
import com.agrievolution.genetics.CropSpecies;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class AEBlocks {
    public static final RegistrationSet<net.minecraft.world.level.block.Block> BLOCKS = new RegistrationSet<>(net.minecraft.core.registries.Registries.BLOCK);
    static { ResourceCropMigration.aliases(BLOCKS, "_crop"); }
    public static final Supplier<IrrigationControllerBlock> IRRIGATION_CONTROLLER = BLOCKS.register("irrigation_controller",
            () -> new IrrigationControllerBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.COPPER)));
    public static final Supplier<ResourceWorkshopBlock> RESOURCE_WORKSHOP = BLOCKS.register("resource_workshop",
            () -> new ResourceWorkshopBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.COPPER)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static final Supplier<BatchCookingBlock> BATCH_COOKING_STATION = BLOCKS.register("batch_cooking_station",
            () -> new BatchCookingBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.COPPER)));
    public static final Supplier<CompostBinBlock> COMPOST_BIN = BLOCKS.register("compost_bin",
            () -> new CompostBinBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD)));
    public static final Supplier<SeedVaultBlock> SEED_VAULT = BLOCKS.register("seed_vault",
            () -> new SeedVaultBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD)));
    public static final Supplier<GardenWorkBlock> GARDEN_ORDER_BOARD = BLOCKS.register("garden_order_board",
            () -> new GardenWorkBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(), false));
    public static final Supplier<GardenWorkBlock> PRESERVING_STATION = BLOCKS.register("preserving_station",
            () -> new GardenWorkBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(), true));

    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of().noCollission().randomTicks()
                .instabreak().sound(SoundType.CROP);
    }

    // Existing ID is unchanged for backward compatibility with 0.3.0 worlds.
    public static final Supplier<AgriCropBlock> AGRI_CROP = BLOCKS.register(
            "agri_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WILD, AEItems.WILD_SEED::get));

    public static final Supplier<AgriCropBlock> CRIMSON_CLOVER_CROP = BLOCKS.register(
            "crimson_clover_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.CRIMSON_CLOVER, AEItems.CRIMSON_CLOVER_SEED::get));

    public static final Supplier<AgriCropBlock> AZURE_FLAX_CROP = BLOCKS.register(
            "azure_flax_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.AZURE_FLAX, AEItems.AZURE_FLAX_SEED::get));

    public static final Supplier<AgriCropBlock> VIOLET_STARFLOWER_CROP = BLOCKS.register(
            "violet_starflower_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.VIOLET_STARFLOWER, AEItems.VIOLET_STARFLOWER_SEED::get));

    public static final Supplier<AgriCropBlock> AMBER_MARIGOLD_CROP = BLOCKS.register(
            "amber_marigold_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.AMBER_MARIGOLD, AEItems.AMBER_MARIGOLD_SEED::get));
    public static final Supplier<AgriCropBlock> SILVER_THISTLE_CROP = BLOCKS.register(
            "silver_thistle_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.SILVER_THISTLE, AEItems.SILVER_THISTLE_SEED::get));
    public static final Supplier<AgriCropBlock> SUNFIRE_LILY_CROP = BLOCKS.register(
            "sunfire_lily_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.SUNFIRE_LILY, AEItems.SUNFIRE_LILY_SEED::get));
    public static final Supplier<AgriCropBlock> MOON_ORCHID_CROP = BLOCKS.register(
            "moon_orchid_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.MOON_ORCHID, AEItems.MOON_ORCHID_SEED::get));
    public static final Supplier<AgriCropBlock> ECLIPSE_LOTUS_CROP = BLOCKS.register(
            "eclipse_lotus_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.ECLIPSE_LOTUS, AEItems.ECLIPSE_LOTUS_SEED::get));
    public static final Supplier<AgriCropBlock> TWILIGHT_SAGE_CROP = BLOCKS.register(
            "twilight_sage_crop", () -> new AgriCropBlock(cropProperties(),
                    CropSpecies.TWILIGHT_SAGE, AEItems.TWILIGHT_SAGE_SEED::get));

    public static final Supplier<AgriCropBlock> DIRT_CROP = BLOCKS.register(
            "dirt_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DIRT, AEItems.DIRT_SEED::get));
    public static final Supplier<AgriCropBlock> WOOD_CROP = BLOCKS.register(
            "wood_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WOOD, AEItems.WOOD_SEED::get));
    public static final Supplier<AgriCropBlock> STONE_CROP = BLOCKS.register(
            "stone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.STONE, AEItems.STONE_SEED::get));
    public static final Supplier<AgriCropBlock> COAL_CROP = BLOCKS.register(
            "coal_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COAL, AEItems.COAL_SEED::get));
    public static final Supplier<AgriCropBlock> COPPER_CROP = BLOCKS.register(
            "copper_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COPPER, AEItems.COPPER_SEED::get));
    public static final Supplier<AgriCropBlock> IRON_CROP = BLOCKS.register(
            "iron_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.IRON, AEItems.IRON_SEED::get));
    public static final Supplier<AgriCropBlock> REDSTONE_CROP = BLOCKS.register(
            "redstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.REDSTONE, AEItems.REDSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> GOLD_CROP = BLOCKS.register(
            "gold_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GOLD, AEItems.GOLD_SEED::get));
    public static final Supplier<AgriCropBlock> LAPIS_CROP = BLOCKS.register(
            "lapis_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LAPIS, AEItems.LAPIS_SEED::get));
    public static final Supplier<AgriCropBlock> DIAMOND_CROP = BLOCKS.register(
            "diamond_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DIAMOND, AEItems.DIAMOND_SEED::get));
    public static final Supplier<AgriCropBlock> EMERALD_CROP = BLOCKS.register(
            "emerald_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.EMERALD, AEItems.EMERALD_SEED::get));
    public static final Supplier<AgriCropBlock> GRAVEL_CROP = BLOCKS.register(
            "gravel_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GRAVEL, AEItems.GRAVEL_SEED::get));
    public static final Supplier<AgriCropBlock> SAND_CROP = BLOCKS.register(
            "sand_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SAND, AEItems.SAND_SEED::get));
    public static final Supplier<AgriCropBlock> CLAY_CROP = BLOCKS.register(
            "clay_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CLAY, AEItems.CLAY_SEED::get));
    public static final Supplier<AgriCropBlock> FLINT_CROP = BLOCKS.register(
            "flint_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FLINT, AEItems.FLINT_SEED::get));
    public static final Supplier<AgriCropBlock> GLOWSTONE_CROP = BLOCKS.register(
            "glowstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GLOWSTONE, AEItems.GLOWSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> OBSIDIAN_CROP = BLOCKS.register(
            "obsidian_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.OBSIDIAN, AEItems.OBSIDIAN_SEED::get));

    public static final Supplier<AgriCropBlock> NETHERRACK_CROP = BLOCKS.register(
            "netherrack_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHERRACK, AEItems.NETHERRACK_SEED::get));
    public static final Supplier<AgriCropBlock> SOUL_SAND_CROP = BLOCKS.register(
            "soul_sand_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SOUL_SAND, AEItems.SOUL_SAND_SEED::get));
    public static final Supplier<AgriCropBlock> MAGMA_CREAM_CROP = BLOCKS.register(
            "magma_cream_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MAGMA_CREAM, AEItems.MAGMA_CREAM_SEED::get));
    public static final Supplier<AgriCropBlock> NETHER_WART_CROP = BLOCKS.register(
            "nether_wart_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHER_WART, AEItems.NETHER_WART_SEED::get));
    public static final Supplier<AgriCropBlock> BLAZE_ROD_CROP = BLOCKS.register(
            "blaze_rod_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLAZE_ROD, AEItems.BLAZE_ROD_SEED::get));
    public static final Supplier<AgriCropBlock> END_STONE_CROP = BLOCKS.register(
            "end_stone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.END_STONE, AEItems.END_STONE_SEED::get));
    public static final Supplier<AgriCropBlock> CHORUS_FRUIT_CROP = BLOCKS.register(
            "chorus_fruit_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHORUS_FRUIT, AEItems.CHORUS_FRUIT_SEED::get));
    public static final Supplier<AgriCropBlock> ENDER_PEARL_CROP = BLOCKS.register(
            "ender_pearl_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ENDER_PEARL, AEItems.ENDER_PEARL_SEED::get));
    public static final Supplier<AgriCropBlock> SHULKER_SHELL_CROP = BLOCKS.register(
            "shulker_shell_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SHULKER_SHELL, AEItems.SHULKER_SHELL_SEED::get));
    public static final Supplier<AgriCropBlock> ANCIENT_DEBRIS_CROP = BLOCKS.register(
            "ancient_debris_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ANCIENT_DEBRIS, AEItems.ANCIENT_DEBRIS_SEED::get));
    public static final Supplier<AgriCropBlock> TOMATO_CROP = BLOCKS.register("tomato_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TOMATO, AEItems.TOMATO_SEED::get));
    public static final Supplier<AgriCropBlock> LETTUCE_CROP = BLOCKS.register("lettuce_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LETTUCE, AEItems.LETTUCE_SEED::get));
    public static final Supplier<AgriCropBlock> CUCUMBER_CROP = BLOCKS.register("cucumber_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CUCUMBER, AEItems.CUCUMBER_SEED::get));
    public static final Supplier<AgriCropBlock> ONION_CROP = BLOCKS.register("onion_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ONION, AEItems.ONION_SEED::get));
    public static final Supplier<AgriCropBlock> GARLIC_CROP = BLOCKS.register("garlic_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GARLIC, AEItems.GARLIC_SEED::get));
    public static final Supplier<AgriCropBlock> PEPPER_CROP = BLOCKS.register("pepper_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PEPPER, AEItems.PEPPER_SEED::get));
    public static final Supplier<AgriCropBlock> STRAWBERRY_CROP = BLOCKS.register("strawberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.STRAWBERRY, AEItems.STRAWBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> RICE_CROP = BLOCKS.register("rice_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RICE, AEItems.RICE_SEED::get));
    public static final Supplier<AgriCropBlock> CORN_CROP = BLOCKS.register("corn_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CORN, AEItems.CORN_SEED::get));
    public static final Supplier<AgriCropBlock> PEAS_CROP = BLOCKS.register("peas_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PEAS, AEItems.PEAS_SEED::get));
    public static final Supplier<AgriCropBlock> BEANS_CROP = BLOCKS.register("beans_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BEANS, AEItems.BEANS_SEED::get));
    public static final Supplier<AgriCropBlock> BROCCOLI_CROP = BLOCKS.register("broccoli_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BROCCOLI, AEItems.BROCCOLI_SEED::get));
    public static final Supplier<AgriCropBlock> CABBAGE_CROP = BLOCKS.register("cabbage_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CABBAGE, AEItems.CABBAGE_SEED::get));
    public static final Supplier<AgriCropBlock> RADISH_CROP = BLOCKS.register("radish_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RADISH, AEItems.RADISH_SEED::get));
    public static final Supplier<AgriCropBlock> AUBERGINE_CROP = BLOCKS.register("aubergine_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.AUBERGINE, AEItems.AUBERGINE_SEED::get));
    public static final Supplier<AgriCropBlock> SWEET_POTATO_CROP = BLOCKS.register("sweet_potato_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SWEET_POTATO, AEItems.SWEET_POTATO_SEED::get));
    public static final Supplier<AgriCropBlock> BASIL_CROP = BLOCKS.register("basil_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BASIL, AEItems.BASIL_SEED::get));
    public static final Supplier<AgriCropBlock> MINT_CROP = BLOCKS.register("mint_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MINT, AEItems.MINT_SEED::get));
    public static final Supplier<AgriCropBlock> ROSEMARY_CROP = BLOCKS.register("rosemary_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ROSEMARY, AEItems.ROSEMARY_SEED::get));
    public static final Supplier<AgriCropBlock> THYME_CROP = BLOCKS.register("thyme_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.THYME, AEItems.THYME_SEED::get));
    public static final Supplier<AgriCropBlock> PARSLEY_CROP = BLOCKS.register("parsley_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PARSLEY, AEItems.PARSLEY_SEED::get));
    public static final Supplier<AgriCropBlock> CORIANDER_CROP = BLOCKS.register("coriander_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CORIANDER, AEItems.CORIANDER_SEED::get));
    public static final Supplier<AgriCropBlock> GINGER_CROP = BLOCKS.register("ginger_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GINGER, AEItems.GINGER_SEED::get));
    public static final Supplier<AgriCropBlock> TURMERIC_CROP = BLOCKS.register("turmeric_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TURMERIC, AEItems.TURMERIC_SEED::get));
    public static final Supplier<AgriCropBlock> STRING_CROP = BLOCKS.register("string_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.STRING, AEItems.STRING_SEED::get));
    public static final Supplier<AgriCropBlock> WOOL_CROP = BLOCKS.register("wool_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WOOL, AEItems.WOOL_SEED::get));
    public static final Supplier<AgriCropBlock> LEATHER_CROP = BLOCKS.register("leather_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LEATHER, AEItems.LEATHER_SEED::get));
    public static final Supplier<AgriCropBlock> BONE_CROP = BLOCKS.register("bone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BONE, AEItems.BONE_SEED::get));
    public static final Supplier<AgriCropBlock> AMETHYST_CROP = BLOCKS.register("amethyst_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.AMETHYST, AEItems.AMETHYST_SEED::get));
    public static final Supplier<AgriCropBlock> PRISMARINE_CROP = BLOCKS.register("prismarine_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PRISMARINE, AEItems.PRISMARINE_SEED::get));
    public static final Supplier<AgriCropBlock> GHAST_TEAR_CROP = BLOCKS.register("ghast_tear_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GHAST_TEAR, AEItems.GHAST_TEAR_SEED::get));
    public static final Supplier<AgriCropBlock> BREEZE_ROD_CROP = BLOCKS.register("breeze_rod_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BREEZE_ROD, AEItems.BREEZE_ROD_SEED::get));
    public static final Supplier<AgriCropBlock> FEATHER_CROP = BLOCKS.register("feather_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FEATHER, AEItems.FEATHER_SEED::get));
    public static final Supplier<AgriCropBlock> INK_SAC_CROP = BLOCKS.register("ink_sac_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.INK_SAC, AEItems.INK_SAC_SEED::get));
    public static final Supplier<AgriCropBlock> HONEYCOMB_CROP = BLOCKS.register("honeycomb_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.HONEYCOMB, AEItems.HONEYCOMB_SEED::get));
    public static final Supplier<AgriCropBlock> SLIME_BALL_CROP = BLOCKS.register("slime_ball_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SLIME_BALL, AEItems.SLIME_BALL_SEED::get));
    public static final Supplier<AgriCropBlock> GUNPOWDER_CROP = BLOCKS.register("gunpowder_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GUNPOWDER, AEItems.GUNPOWDER_SEED::get));
    public static final Supplier<AgriCropBlock> PRISMARINE_CRYSTALS_CROP = BLOCKS.register("prismarine_crystals_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PRISMARINE_CRYSTALS, AEItems.PRISMARINE_CRYSTALS_SEED::get));
    public static final Supplier<AgriCropBlock> PHANTOM_MEMBRANE_CROP = BLOCKS.register("phantom_membrane_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PHANTOM_MEMBRANE, AEItems.PHANTOM_MEMBRANE_SEED::get));
    public static final Supplier<AgriCropBlock> ECHO_SHARD_CROP = BLOCKS.register("echo_shard_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ECHO_SHARD, AEItems.ECHO_SHARD_SEED::get));
    public static final Supplier<AgriCropBlock> SNOWBALL_CROP = BLOCKS.register("snowball_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SNOWBALL, AEItems.SNOWBALL_SEED::get));
    public static final Supplier<AgriCropBlock> MOSS_CROP = BLOCKS.register("moss_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MOSS, AEItems.MOSS_SEED::get));
    public static final Supplier<AgriCropBlock> CALCITE_CROP = BLOCKS.register("calcite_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CALCITE, AEItems.CALCITE_SEED::get));
    public static final Supplier<AgriCropBlock> TERRACOTTA_CROP = BLOCKS.register("terracotta_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TERRACOTTA, AEItems.TERRACOTTA_SEED::get));
    public static final Supplier<AgriCropBlock> SPIDER_EYE_CROP = BLOCKS.register("spider_eye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPIDER_EYE, AEItems.SPIDER_EYE_SEED::get));
    public static final Supplier<AgriCropBlock> GLOW_INK_SAC_CROP = BLOCKS.register("glow_ink_sac_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GLOW_INK_SAC, AEItems.GLOW_INK_SAC_SEED::get));
    public static final Supplier<AgriCropBlock> CRYING_OBSIDIAN_CROP = BLOCKS.register("crying_obsidian_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CRYING_OBSIDIAN, AEItems.CRYING_OBSIDIAN_SEED::get));
    public static final Supplier<AgriCropBlock> NAUTILUS_SHELL_CROP = BLOCKS.register("nautilus_shell_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NAUTILUS_SHELL, AEItems.NAUTILUS_SHELL_SEED::get));
    public static final Supplier<AgriCropBlock> PAPER_CROP = BLOCKS.register("paper_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PAPER, AEItems.PAPER_SEED::get));
    public static final Supplier<AgriCropBlock> KELP_CROP = BLOCKS.register("kelp_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.KELP, AEItems.KELP_SEED::get));
    public static final Supplier<AgriCropBlock> BAMBOO_CROP = BLOCKS.register("bamboo_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BAMBOO, AEItems.BAMBOO_SEED::get));
    public static final Supplier<AgriCropBlock> BLACKSTONE_CROP = BLOCKS.register("blackstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLACKSTONE, AEItems.BLACKSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> BASALT_CROP = BLOCKS.register("basalt_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BASALT, AEItems.BASALT_SEED::get));
    public static final Supplier<AgriCropBlock> PURPUR_CROP = BLOCKS.register("purpur_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PURPUR, AEItems.PURPUR_SEED::get));
    public static final Supplier<AgriCropBlock> TURTLE_SCUTE_CROP = BLOCKS.register("turtle_scute_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TURTLE_SCUTE, AEItems.TURTLE_SCUTE_SEED::get));
    public static final Supplier<AgriCropBlock> SPONGE_CROP = BLOCKS.register("sponge_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPONGE, AEItems.SPONGE_SEED::get));
    public static final Supplier<AgriCropBlock> GRANITE_CROP = BLOCKS.register("granite_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GRANITE, AEItems.GRANITE_SEED::get));
    public static final Supplier<AgriCropBlock> DIORITE_CROP = BLOCKS.register("diorite_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DIORITE, AEItems.DIORITE_SEED::get));
    public static final Supplier<AgriCropBlock> ANDESITE_CROP = BLOCKS.register("andesite_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ANDESITE, AEItems.ANDESITE_SEED::get));
    public static final Supplier<AgriCropBlock> MUD_CROP = BLOCKS.register("mud_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MUD, AEItems.MUD_SEED::get));
    public static final Supplier<AgriCropBlock> TUFF_CROP = BLOCKS.register("tuff_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TUFF, AEItems.TUFF_SEED::get));
    public static final Supplier<AgriCropBlock> DRIPSTONE_CROP = BLOCKS.register("dripstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DRIPSTONE, AEItems.DRIPSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> DEEPSLATE_CROP = BLOCKS.register("deepslate_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DEEPSLATE, AEItems.DEEPSLATE_SEED::get));
    public static final Supplier<AgriCropBlock> SOUL_SOIL_CROP = BLOCKS.register("soul_soil_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SOUL_SOIL, AEItems.SOUL_SOIL_SEED::get));
    public static final Supplier<AgriCropBlock> SEA_LANTERN_CROP = BLOCKS.register("sea_lantern_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SEA_LANTERN, AEItems.SEA_LANTERN_SEED::get));
    public static final Supplier<AgriCropBlock> SHROOMLIGHT_CROP = BLOCKS.register("shroomlight_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SHROOMLIGHT, AEItems.SHROOMLIGHT_SEED::get));
    public static final Supplier<AgriCropBlock> END_ROD_CROP = BLOCKS.register("end_rod_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.END_ROD, AEItems.END_ROD_SEED::get));
    public static final Supplier<AgriCropBlock> SCULK_CROP = BLOCKS.register("sculk_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SCULK, AEItems.SCULK_SEED::get));
    public static final Supplier<AgriCropBlock> RED_SAND_CROP = BLOCKS.register("red_sand_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RED_SAND, AEItems.RED_SAND_SEED::get));
    public static final Supplier<AgriCropBlock> SANDSTONE_CROP = BLOCKS.register("sandstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SANDSTONE, AEItems.SANDSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> CACTUS_CROP = BLOCKS.register("cactus_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CACTUS, AEItems.CACTUS_SEED::get));
    public static final Supplier<AgriCropBlock> VINE_CROP = BLOCKS.register("vine_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.VINE, AEItems.VINE_SEED::get));
    public static final Supplier<AgriCropBlock> PACKED_MUD_CROP = BLOCKS.register("packed_mud_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PACKED_MUD, AEItems.PACKED_MUD_SEED::get));
    public static final Supplier<AgriCropBlock> BRICK_CROP = BLOCKS.register("brick_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BRICK, AEItems.BRICK_SEED::get));
    public static final Supplier<AgriCropBlock> ICE_CROP = BLOCKS.register("ice_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ICE, AEItems.ICE_SEED::get));
    public static final Supplier<AgriCropBlock> PACKED_ICE_CROP = BLOCKS.register("packed_ice_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PACKED_ICE, AEItems.PACKED_ICE_SEED::get));
    public static final Supplier<AgriCropBlock> BLUE_ICE_CROP = BLOCKS.register("blue_ice_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLUE_ICE, AEItems.BLUE_ICE_SEED::get));
    public static final Supplier<AgriCropBlock> NETHER_BRICK_CROP = BLOCKS.register("nether_brick_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHER_BRICK, AEItems.NETHER_BRICK_SEED::get));
    public static final Supplier<AgriCropBlock> ENDER_EYE_CROP = BLOCKS.register("ender_eye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ENDER_EYE, AEItems.ENDER_EYE_SEED::get));
    public static final Supplier<AgriCropBlock> CHARCOAL_CROP = BLOCKS.register("charcoal_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHARCOAL, AEItems.CHARCOAL_SEED::get));
    public static final Supplier<AgriCropBlock> GLASS_CROP = BLOCKS.register("glass_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GLASS, AEItems.GLASS_SEED::get));
    public static final Supplier<AgriCropBlock> BROWN_MUSHROOM_CROP = BLOCKS.register("brown_mushroom_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BROWN_MUSHROOM, AEItems.BROWN_MUSHROOM_SEED::get));
    public static final Supplier<AgriCropBlock> RED_MUSHROOM_CROP = BLOCKS.register("red_mushroom_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RED_MUSHROOM, AEItems.RED_MUSHROOM_SEED::get));
    public static final Supplier<AgriCropBlock> MOSSY_COBBLESTONE_CROP = BLOCKS.register("mossy_cobblestone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MOSSY_COBBLESTONE, AEItems.MOSSY_COBBLESTONE_SEED::get));
    public static final Supplier<AgriCropBlock> LILY_PAD_CROP = BLOCKS.register("lily_pad_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LILY_PAD, AEItems.LILY_PAD_SEED::get));
    public static final Supplier<AgriCropBlock> DARK_PRISMARINE_CROP = BLOCKS.register("dark_prismarine_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DARK_PRISMARINE, AEItems.DARK_PRISMARINE_SEED::get));
    public static final Supplier<AgriCropBlock> GILDED_BLACKSTONE_CROP = BLOCKS.register("gilded_blackstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GILDED_BLACKSTONE, AEItems.GILDED_BLACKSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> SCULK_CATALYST_CROP = BLOCKS.register("sculk_catalyst_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SCULK_CATALYST, AEItems.SCULK_CATALYST_SEED::get));
    public static final Supplier<AgriCropBlock> OCHRE_FROGLIGHT_CROP = BLOCKS.register("ochre_froglight_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.OCHRE_FROGLIGHT, AEItems.OCHRE_FROGLIGHT_SEED::get));
    public static final Supplier<AgriCropBlock> SPRUCE_LOG_CROP = BLOCKS.register("spruce_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPRUCE_LOG, AEItems.SPRUCE_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> BIRCH_LOG_CROP = BLOCKS.register("birch_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BIRCH_LOG, AEItems.BIRCH_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> ACACIA_LOG_CROP = BLOCKS.register("acacia_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ACACIA_LOG, AEItems.ACACIA_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> DARK_OAK_LOG_CROP = BLOCKS.register("dark_oak_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DARK_OAK_LOG, AEItems.DARK_OAK_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> MANGROVE_LOG_CROP = BLOCKS.register("mangrove_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MANGROVE_LOG, AEItems.MANGROVE_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> CHERRY_LOG_CROP = BLOCKS.register("cherry_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHERRY_LOG, AEItems.CHERRY_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> SEA_PICKLE_CROP = BLOCKS.register("sea_pickle_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SEA_PICKLE, AEItems.SEA_PICKLE_SEED::get));
    public static final Supplier<AgriCropBlock> GLOW_LICHEN_CROP = BLOCKS.register("glow_lichen_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GLOW_LICHEN, AEItems.GLOW_LICHEN_SEED::get));
    public static final Supplier<AgriCropBlock> CRIMSON_STEM_CROP = BLOCKS.register("crimson_stem_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CRIMSON_STEM, AEItems.CRIMSON_STEM_SEED::get));
    public static final Supplier<AgriCropBlock> WARPED_STEM_CROP = BLOCKS.register("warped_stem_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WARPED_STEM, AEItems.WARPED_STEM_SEED::get));
    public static final Supplier<AgriCropBlock> VERDANT_FROGLIGHT_CROP = BLOCKS.register("verdant_froglight_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.VERDANT_FROGLIGHT, AEItems.VERDANT_FROGLIGHT_SEED::get));
    public static final Supplier<AgriCropBlock> PEARLESCENT_FROGLIGHT_CROP = BLOCKS.register("pearlescent_froglight_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PEARLESCENT_FROGLIGHT, AEItems.PEARLESCENT_FROGLIGHT_SEED::get));
    public static final Supplier<AgriCropBlock> SUGAR_CROP = BLOCKS.register("sugar_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SUGAR, AEItems.SUGAR_SEED::get));
    public static final Supplier<AgriCropBlock> COCOA_BEANS_CROP = BLOCKS.register("cocoa_beans_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COCOA_BEANS, AEItems.COCOA_BEANS_SEED::get));
    public static final Supplier<AgriCropBlock> EGG_CROP = BLOCKS.register("egg_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.EGG, AEItems.EGG_SEED::get));
    public static final Supplier<AgriCropBlock> ROTTEN_FLESH_CROP = BLOCKS.register("rotten_flesh_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ROTTEN_FLESH, AEItems.ROTTEN_FLESH_SEED::get));
    public static final Supplier<AgriCropBlock> RABBIT_HIDE_CROP = BLOCKS.register("rabbit_hide_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RABBIT_HIDE, AEItems.RABBIT_HIDE_SEED::get));
    public static final Supplier<AgriCropBlock> RABBIT_FOOT_CROP = BLOCKS.register("rabbit_foot_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RABBIT_FOOT, AEItems.RABBIT_FOOT_SEED::get));
    public static final Supplier<AgriCropBlock> ARMADILLO_SCUTE_CROP = BLOCKS.register("armadillo_scute_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ARMADILLO_SCUTE, AEItems.ARMADILLO_SCUTE_SEED::get));
    public static final Supplier<AgriCropBlock> FERMENTED_SPIDER_EYE_CROP = BLOCKS.register("fermented_spider_eye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FERMENTED_SPIDER_EYE, AEItems.FERMENTED_SPIDER_EYE_SEED::get));
    public static final Supplier<AgriCropBlock> WITHER_ROSE_CROP = BLOCKS.register("wither_rose_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WITHER_ROSE, AEItems.WITHER_ROSE_SEED::get));
    public static final Supplier<AgriCropBlock> CHORUS_FLOWER_CROP = BLOCKS.register("chorus_flower_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHORUS_FLOWER, AEItems.CHORUS_FLOWER_SEED::get));
    public static final Supplier<AgriCropBlock> CRIMSON_FUNGUS_CROP = BLOCKS.register("crimson_fungus_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CRIMSON_FUNGUS, AEItems.CRIMSON_FUNGUS_SEED::get));
    public static final Supplier<AgriCropBlock> WARPED_FUNGUS_CROP = BLOCKS.register("warped_fungus_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WARPED_FUNGUS, AEItems.WARPED_FUNGUS_SEED::get));
    public static final Supplier<AgriCropBlock> GREEN_DYE_CROP = BLOCKS.register("green_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GREEN_DYE, AEItems.GREEN_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> RED_DYE_CROP = BLOCKS.register("red_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RED_DYE, AEItems.RED_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> YELLOW_DYE_CROP = BLOCKS.register("yellow_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.YELLOW_DYE, AEItems.YELLOW_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> BROWN_DYE_CROP = BLOCKS.register("brown_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BROWN_DYE, AEItems.BROWN_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> HONEY_BOTTLE_CROP = BLOCKS.register("honey_bottle_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.HONEY_BOTTLE, AEItems.HONEY_BOTTLE_SEED::get));
    public static final Supplier<AgriCropBlock> GLOW_BERRIES_CROP = BLOCKS.register("glow_berries_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GLOW_BERRIES, AEItems.GLOW_BERRIES_SEED::get));
    public static final Supplier<AgriCropBlock> BLUE_DYE_CROP = BLOCKS.register("blue_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLUE_DYE, AEItems.BLUE_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> CYAN_DYE_CROP = BLOCKS.register("cyan_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CYAN_DYE, AEItems.CYAN_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> FIRE_CHARGE_CROP = BLOCKS.register("fire_charge_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FIRE_CHARGE, AEItems.FIRE_CHARGE_SEED::get));
    public static final Supplier<AgriCropBlock> WIND_CHARGE_CROP = BLOCKS.register("wind_charge_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WIND_CHARGE, AEItems.WIND_CHARGE_SEED::get));
    public static final Supplier<AgriCropBlock> DRAGON_BREATH_CROP = BLOCKS.register("dragon_breath_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DRAGON_BREATH, AEItems.DRAGON_BREATH_SEED::get));
    public static final Supplier<AgriCropBlock> EXPERIENCE_BOTTLE_CROP = BLOCKS.register("experience_bottle_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.EXPERIENCE_BOTTLE, AEItems.EXPERIENCE_BOTTLE_SEED::get));
    public static final Supplier<AgriCropBlock> WHITE_DYE_CROP = BLOCKS.register("white_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WHITE_DYE, AEItems.WHITE_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> BLACK_DYE_CROP = BLOCKS.register("black_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLACK_DYE, AEItems.BLACK_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> ORANGE_DYE_CROP = BLOCKS.register("orange_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ORANGE_DYE, AEItems.ORANGE_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> PINK_DYE_CROP = BLOCKS.register("pink_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PINK_DYE, AEItems.PINK_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> PURPLE_DYE_CROP = BLOCKS.register("purple_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PURPLE_DYE, AEItems.PURPLE_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> MAGENTA_DYE_CROP = BLOCKS.register("magenta_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MAGENTA_DYE, AEItems.MAGENTA_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> LIGHT_BLUE_DYE_CROP = BLOCKS.register("light_blue_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LIGHT_BLUE_DYE, AEItems.LIGHT_BLUE_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> LIME_DYE_CROP = BLOCKS.register("lime_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LIME_DYE, AEItems.LIME_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> HEART_OF_THE_SEA_CROP = BLOCKS.register("heart_of_the_sea_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.HEART_OF_THE_SEA, AEItems.HEART_OF_THE_SEA_SEED::get));
    public static final Supplier<AgriCropBlock> NETHER_STAR_CROP = BLOCKS.register("nether_star_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHER_STAR, AEItems.NETHER_STAR_SEED::get));
    public static final Supplier<AgriCropBlock> DRAGON_HEAD_CROP = BLOCKS.register("dragon_head_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DRAGON_HEAD, AEItems.DRAGON_HEAD_SEED::get));
    public static final Supplier<AgriCropBlock> HEAVY_CORE_CROP = BLOCKS.register("heavy_core_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.HEAVY_CORE, AEItems.HEAVY_CORE_SEED::get));
    public static final Supplier<AgriCropBlock> STICK_CROP = BLOCKS.register("stick_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.STICK, AEItems.STICK_SEED::get));
    public static final Supplier<AgriCropBlock> DRIED_KELP_CROP = BLOCKS.register("dried_kelp_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DRIED_KELP, AEItems.DRIED_KELP_SEED::get));
    public static final Supplier<AgriCropBlock> GRAY_DYE_CROP = BLOCKS.register("gray_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GRAY_DYE, AEItems.GRAY_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> LIGHT_GRAY_DYE_CROP = BLOCKS.register("light_gray_dye_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LIGHT_GRAY_DYE, AEItems.LIGHT_GRAY_DYE_SEED::get));
    public static final Supplier<AgriCropBlock> SKELETON_SKULL_CROP = BLOCKS.register("skeleton_skull_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SKELETON_SKULL, AEItems.SKELETON_SKULL_SEED::get));
    public static final Supplier<AgriCropBlock> WITHER_SKELETON_SKULL_CROP = BLOCKS.register("wither_skeleton_skull_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WITHER_SKELETON_SKULL, AEItems.WITHER_SKELETON_SKULL_SEED::get));
    public static final Supplier<AgriCropBlock> CREEPER_HEAD_CROP = BLOCKS.register("creeper_head_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CREEPER_HEAD, AEItems.CREEPER_HEAD_SEED::get));
    public static final Supplier<AgriCropBlock> ZOMBIE_HEAD_CROP = BLOCKS.register("zombie_head_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ZOMBIE_HEAD, AEItems.ZOMBIE_HEAD_SEED::get));
    public static final Supplier<AgriCropBlock> PUMPKIN_CROP = BLOCKS.register("pumpkin_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PUMPKIN, AEItems.PUMPKIN_SEED::get));
    public static final Supplier<AgriCropBlock> BUTTERNUT_SQUASH_CROP = BLOCKS.register("butternut_squash_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BUTTERNUT_SQUASH, AEItems.BUTTERNUT_SQUASH_SEED::get));
    public static final Supplier<AgriCropBlock> COURGETTE_CROP = BLOCKS.register("courgette_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COURGETTE, AEItems.COURGETTE_SEED::get));
    public static final Supplier<AgriCropBlock> TURNIP_CROP = BLOCKS.register("turnip_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TURNIP, AEItems.TURNIP_SEED::get));
    public static final Supplier<AgriCropBlock> PARSNIP_CROP = BLOCKS.register("parsnip_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PARSNIP, AEItems.PARSNIP_SEED::get));
    public static final Supplier<AgriCropBlock> LEEK_CROP = BLOCKS.register("leek_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LEEK, AEItems.LEEK_SEED::get));
    public static final Supplier<AgriCropBlock> CELERY_CROP = BLOCKS.register("celery_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CELERY, AEItems.CELERY_SEED::get));
    public static final Supplier<AgriCropBlock> ASPARAGUS_CROP = BLOCKS.register("asparagus_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ASPARAGUS, AEItems.ASPARAGUS_SEED::get));
    public static final Supplier<AgriCropBlock> BLUEBERRY_CROP = BLOCKS.register("blueberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLUEBERRY, AEItems.BLUEBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> RASPBERRY_CROP = BLOCKS.register("raspberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RASPBERRY, AEItems.RASPBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> BLACKBERRY_CROP = BLOCKS.register("blackberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLACKBERRY, AEItems.BLACKBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> CRANBERRY_CROP = BLOCKS.register("cranberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CRANBERRY, AEItems.CRANBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> SPINACH_CROP = BLOCKS.register("spinach_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPINACH, AEItems.SPINACH_SEED::get));
    public static final Supplier<AgriCropBlock> CAULIFLOWER_CROP = BLOCKS.register("cauliflower_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CAULIFLOWER, AEItems.CAULIFLOWER_SEED::get));
    public static final Supplier<AgriCropBlock> ARTICHOKE_CROP = BLOCKS.register("artichoke_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ARTICHOKE, AEItems.ARTICHOKE_SEED::get));
    public static final Supplier<AgriCropBlock> CHILLI_CROP = BLOCKS.register("chilli_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHILLI, AEItems.CHILLI_SEED::get));
    public static final Supplier<AgriCropBlock> SPRING_ONION_CROP = BLOCKS.register("spring_onion_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPRING_ONION, AEItems.SPRING_ONION_SEED::get));
    public static final Supplier<AgriCropBlock> FENNEL_CROP = BLOCKS.register("fennel_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FENNEL, AEItems.FENNEL_SEED::get));
    public static final Supplier<AgriCropBlock> DILL_CROP = BLOCKS.register("dill_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DILL, AEItems.DILL_SEED::get));
    public static final Supplier<AgriCropBlock> PEANUT_CROP = BLOCKS.register("peanut_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PEANUT, AEItems.PEANUT_SEED::get));
    public static final Supplier<AgriCropBlock> SESAME_CROP = BLOCKS.register("sesame_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SESAME, AEItems.SESAME_SEED::get));
    public static final Supplier<AgriCropBlock> OATS_CROP = BLOCKS.register("oats_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.OATS, AEItems.OATS_SEED::get));
    public static final Supplier<AgriCropBlock> GOOSEBERRY_CROP = BLOCKS.register("gooseberry_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GOOSEBERRY, AEItems.GOOSEBERRY_SEED::get));
    public static final Supplier<AgriCropBlock> BLACKCURRANT_CROP = BLOCKS.register("blackcurrant_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BLACKCURRANT, AEItems.BLACKCURRANT_SEED::get));
    public static final Supplier<AgriCropBlock> KALE_CROP = BLOCKS.register("kale_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.KALE, AEItems.KALE_SEED::get));
    public static final Supplier<AgriCropBlock> PAK_CHOI_CROP = BLOCKS.register("pak_choi_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PAK_CHOI, AEItems.PAK_CHOI_SEED::get));
    public static final Supplier<AgriCropBlock> BRUSSELS_SPROUTS_CROP = BLOCKS.register("brussels_sprouts_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BRUSSELS_SPROUTS, AEItems.BRUSSELS_SPROUTS_SEED::get));
    public static final Supplier<AgriCropBlock> SWEDE_CROP = BLOCKS.register("swede_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SWEDE, AEItems.SWEDE_SEED::get));
    public static final Supplier<AgriCropBlock> CELERIAC_CROP = BLOCKS.register("celeriac_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CELERIAC, AEItems.CELERIAC_SEED::get));
    public static final Supplier<AgriCropBlock> LENTIL_CROP = BLOCKS.register("lentil_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.LENTIL, AEItems.LENTIL_SEED::get));
    public static final Supplier<AgriCropBlock> CHICKPEA_CROP = BLOCKS.register("chickpea_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CHICKPEA, AEItems.CHICKPEA_SEED::get));
    public static final Supplier<AgriCropBlock> BARLEY_CROP = BLOCKS.register("barley_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BARLEY, AEItems.BARLEY_SEED::get));
    public static final Supplier<AgriCropBlock> BUCKWHEAT_CROP = BLOCKS.register("buckwheat_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BUCKWHEAT, AEItems.BUCKWHEAT_SEED::get));
    public static final Supplier<AgriCropBlock> RHUBARB_CROP = BLOCKS.register("rhubarb_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RHUBARB, AEItems.RHUBARB_SEED::get));
    public static final Supplier<AgriCropBlock> GRAPE_CROP = BLOCKS.register("grape_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GRAPE, AEItems.GRAPE_SEED::get));
    public static final Supplier<AgriCropBlock> CANTALOUPE_CROP = BLOCKS.register("cantaloupe_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CANTALOUPE, AEItems.CANTALOUPE_SEED::get));
    public static final Supplier<AgriCropBlock> JUNGLE_LOG_CROP = BLOCKS.register("jungle_log_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.JUNGLE_LOG, AEItems.JUNGLE_LOG_SEED::get));
    public static final Supplier<AgriCropBlock> COARSE_DIRT_CROP = BLOCKS.register("coarse_dirt_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COARSE_DIRT, AEItems.COARSE_DIRT_SEED::get));
    public static final Supplier<AgriCropBlock> ROOTED_DIRT_CROP = BLOCKS.register("rooted_dirt_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ROOTED_DIRT, AEItems.ROOTED_DIRT_SEED::get));
    public static final Supplier<AgriCropBlock> RED_SANDSTONE_CROP = BLOCKS.register("red_sandstone_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RED_SANDSTONE, AEItems.RED_SANDSTONE_SEED::get));
    public static final Supplier<AgriCropBlock> PRISMARINE_BRICKS_CROP = BLOCKS.register("prismarine_bricks_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PRISMARINE_BRICKS, AEItems.PRISMARINE_BRICKS_SEED::get));
    public static final Supplier<AgriCropBlock> WARPED_WART_BLOCK_CROP = BLOCKS.register("warped_wart_block_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.WARPED_WART_BLOCK, AEItems.WARPED_WART_BLOCK_SEED::get));
    public static final Supplier<AgriCropBlock> RED_NETHER_BRICKS_CROP = BLOCKS.register("red_nether_bricks_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RED_NETHER_BRICKS, AEItems.RED_NETHER_BRICKS_SEED::get));
    public static final Supplier<AgriCropBlock> SCULK_SENSOR_CROP = BLOCKS.register("sculk_sensor_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SCULK_SENSOR, AEItems.SCULK_SENSOR_SEED::get));
    public static final Supplier<AgriCropBlock> SCULK_SHRIEKER_CROP = BLOCKS.register("sculk_shrieker_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SCULK_SHRIEKER, AEItems.SCULK_SHRIEKER_SEED::get));
    public static final Supplier<AgriCropBlock> CONDUIT_CROP = BLOCKS.register("conduit_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CONDUIT, AEItems.CONDUIT_SEED::get));
    public static final Supplier<AgriCropBlock> RESPAWN_ANCHOR_CROP = BLOCKS.register("respawn_anchor_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.RESPAWN_ANCHOR, AEItems.RESPAWN_ANCHOR_SEED::get));
    public static final Supplier<AgriCropBlock> GRASS_BLOCK_CROP = BLOCKS.register("grass_block_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.GRASS_BLOCK, AEItems.GRASS_BLOCK_SEED::get));
    public static final Supplier<AgriCropBlock> PODZOL_CROP = BLOCKS.register("podzol_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PODZOL, AEItems.PODZOL_SEED::get));
    public static final Supplier<AgriCropBlock> MYCELIUM_CROP = BLOCKS.register("mycelium_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MYCELIUM, AEItems.MYCELIUM_SEED::get));
    public static final Supplier<AgriCropBlock> DEAD_BUSH_CROP = BLOCKS.register("dead_bush_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DEAD_BUSH, AEItems.DEAD_BUSH_SEED::get));
    public static final Supplier<AgriCropBlock> MUD_BRICKS_CROP = BLOCKS.register("mud_bricks_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.MUD_BRICKS, AEItems.MUD_BRICKS_SEED::get));
    public static final Supplier<AgriCropBlock> DEEPSLATE_BRICKS_CROP = BLOCKS.register("deepslate_bricks_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DEEPSLATE_BRICKS, AEItems.DEEPSLATE_BRICKS_SEED::get));
    public static final Supplier<AgriCropBlock> CALIBRATED_SCULK_SENSOR_CROP = BLOCKS.register("calibrated_sculk_sensor_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.CALIBRATED_SCULK_SENSOR, AEItems.CALIBRATED_SCULK_SENSOR_SEED::get));
    public static final Supplier<AgriCropBlock> END_CRYSTAL_CROP = BLOCKS.register("end_crystal_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.END_CRYSTAL, AEItems.END_CRYSTAL_SEED::get));
    public static final Supplier<AgriCropBlock> TOTEM_OF_UNDYING_CROP = BLOCKS.register("totem_of_undying_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TOTEM_OF_UNDYING, AEItems.TOTEM_OF_UNDYING_SEED::get));
    public static final Supplier<AgriCropBlock> ENCHANTED_GOLDEN_APPLE_CROP = BLOCKS.register("enchanted_golden_apple_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.ENCHANTED_GOLDEN_APPLE, AEItems.ENCHANTED_GOLDEN_APPLE_SEED::get));
    public static final Supplier<AgriCropBlock> SUGAR_CANE_CROP = BLOCKS.register("sugar_cane_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SUGAR_CANE, AEItems.SUGAR_CANE_SEED::get));
    public static final Supplier<AgriCropBlock> SWEET_BERRIES_CROP = BLOCKS.register("sweet_berries_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SWEET_BERRIES, AEItems.SWEET_BERRIES_SEED::get));
    public static final Supplier<AgriCropBlock> SEAGRASS_CROP = BLOCKS.register("seagrass_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SEAGRASS, AEItems.SEAGRASS_SEED::get));
    public static final Supplier<AgriCropBlock> HANGING_ROOTS_CROP = BLOCKS.register("hanging_roots_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.HANGING_ROOTS, AEItems.HANGING_ROOTS_SEED::get));
    public static final Supplier<AgriCropBlock> TORCHFLOWER_CROP = BLOCKS.register("torchflower_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TORCHFLOWER, AEItems.TORCHFLOWER_SEED::get));
    public static final Supplier<AgriCropBlock> PITCHER_PLANT_CROP = BLOCKS.register("pitcher_plant_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.PITCHER_PLANT, AEItems.PITCHER_PLANT_SEED::get));
    public static final Supplier<AgriCropBlock> SPORE_BLOSSOM_CROP = BLOCKS.register("spore_blossom_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPORE_BLOSSOM, AEItems.SPORE_BLOSSOM_SEED::get));
    public static final Supplier<AgriCropBlock> BIG_DRIPLEAF_CROP = BLOCKS.register("big_dripleaf_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BIG_DRIPLEAF, AEItems.BIG_DRIPLEAF_SEED::get));
    public static final Supplier<AgriCropBlock> TRIAL_KEY_CROP = BLOCKS.register("trial_key_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TRIAL_KEY, AEItems.TRIAL_KEY_SEED::get));
    public static final Supplier<AgriCropBlock> OMINOUS_TRIAL_KEY_CROP = BLOCKS.register("ominous_trial_key_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.OMINOUS_TRIAL_KEY, AEItems.OMINOUS_TRIAL_KEY_SEED::get));
    public static final Supplier<AgriCropBlock> NETHERITE_UPGRADE_SMITHING_TEMPLATE_CROP = BLOCKS.register("netherite_upgrade_smithing_template_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHERITE_UPGRADE_SMITHING_TEMPLATE, AEItems.NETHERITE_UPGRADE_SMITHING_TEMPLATE_SEED::get));
    public static final Supplier<AgriCropBlock> DISC_FRAGMENT_5_CROP = BLOCKS.register("disc_fragment_5_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DISC_FRAGMENT_5, AEItems.DISC_FRAGMENT_5_SEED::get));
    public static final Supplier<AgriCropBlock> OAK_SAPLING_CROP = BLOCKS.register("oak_sapling_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.OAK_SAPLING, AEItems.OAK_SAPLING_SEED::get));
    public static final Supplier<AgriCropBlock> SPRUCE_SAPLING_CROP = BLOCKS.register("spruce_sapling_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SPRUCE_SAPLING, AEItems.SPRUCE_SAPLING_SEED::get));
    public static final Supplier<AgriCropBlock> BIRCH_SAPLING_CROP = BLOCKS.register("birch_sapling_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BIRCH_SAPLING, AEItems.BIRCH_SAPLING_SEED::get));
    public static final Supplier<AgriCropBlock> DARK_OAK_SAPLING_CROP = BLOCKS.register("dark_oak_sapling_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.DARK_OAK_SAPLING, AEItems.DARK_OAK_SAPLING_SEED::get));
    public static final Supplier<AgriCropBlock> TUBE_CORAL_CROP = BLOCKS.register("tube_coral_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TUBE_CORAL, AEItems.TUBE_CORAL_SEED::get));
    public static final Supplier<AgriCropBlock> BRAIN_CORAL_CROP = BLOCKS.register("brain_coral_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BRAIN_CORAL, AEItems.BRAIN_CORAL_SEED::get));
    public static final Supplier<AgriCropBlock> BUBBLE_CORAL_CROP = BLOCKS.register("bubble_coral_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.BUBBLE_CORAL, AEItems.BUBBLE_CORAL_SEED::get));
    public static final Supplier<AgriCropBlock> FIRE_CORAL_CROP = BLOCKS.register("fire_coral_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.FIRE_CORAL, AEItems.FIRE_CORAL_SEED::get));
    public static final Supplier<AgriCropBlock> COAST_ARMOR_TRIM_SMITHING_TEMPLATE_CROP = BLOCKS.register("coast_armor_trim_smithing_template_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, AEItems.COAST_ARMOR_TRIM_SMITHING_TEMPLATE_SEED::get));
    public static final Supplier<AgriCropBlock> TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP = BLOCKS.register("tide_armor_trim_smithing_template_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, AEItems.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED::get));
    public static final Supplier<AgriCropBlock> SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_CROP = BLOCKS.register("snout_armor_trim_smithing_template_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, AEItems.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_SEED::get));
    public static final Supplier<AgriCropBlock> SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP = BLOCKS.register("silence_armor_trim_smithing_template_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, AEItems.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED::get));
    public static final Supplier<AgriCropBlock> NETHERITE_CROP = BLOCKS.register(
            "netherite_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.NETHERITE, AEItems.NETHERITE_SEED::get));

    public static final Supplier<AgriCropBlock> QUARTZ_CROP = BLOCKS.register(
            "quartz_crop", () -> new AgriCropBlock(cropProperties(), CropSpecies.QUARTZ, AEItems.QUARTZ_SEED::get));

    public static final Supplier<IrrigationBasinBlock> IRRIGATION_BASIN = BLOCKS.register(
            "irrigation_basin", () -> new IrrigationBasinBlock(BlockBehaviour.Properties.of()
                    .strength(1.3f).sound(SoundType.WOOD).noOcclusion()));

    // Standalone desktop analyzer: no old crop/basin IDs are changed.
    public static final Supplier<SeedAnalyzerBlock> SEED_ANALYZER = BLOCKS.register(
            "seed_analyzer", () -> new SeedAnalyzerBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f).sound(SoundType.METAL)));

    public static final Supplier<SeedBreedingStationBlock> SEED_BREEDING_STATION = BLOCKS.register(
            "seed_breeding_station", () -> new SeedBreedingStationBlock(BlockBehaviour.Properties.of()
                    .strength(2.8f).sound(SoundType.WOOD)));

    public static final Supplier<BotanicalGreenhouseBlock> BOTANICAL_GREENHOUSE = BLOCKS.register(
            "botanical_greenhouse", () -> new BotanicalGreenhouseBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f).sound(SoundType.METAL)));

    public static final Supplier<VerticalGreenhouseBlock> VERTICAL_GREENHOUSE = BLOCKS.register(
            "vertical_greenhouse", () -> new VerticalGreenhouseBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    public static final Supplier<BotanicalGeneticsLaboratoryBlock> BOTANICAL_GENETICS_LABORATORY = BLOCKS.register(
            "botanical_genetics_laboratory", () -> new BotanicalGeneticsLaboratoryBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f).sound(SoundType.METAL)));

    // A workstation for consuming actual soil specimens and refining new farmland items.
    public static final Supplier<SoilResearchTableBlock> SOIL_RESEARCH_TABLE = BLOCKS.register(
            "soil_research_table", () -> new SoilResearchTableBlock(BlockBehaviour.Properties.of()
                    .strength(2.4f).sound(SoundType.WOOD)));

    // Researchable, placeable coloured farmland. Existing farmland/crop IDs are not modified.
    public static final Supplier<AdvancedFarmlandBlock> CULTIVATED_FARMLAND = BLOCKS.register(
            "cultivated_farmland", () -> new AdvancedFarmlandBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f).sound(SoundType.GRAVEL).randomTicks(), 1));
    public static final Supplier<AdvancedFarmlandBlock> NUTRIENT_FARMLAND = BLOCKS.register(
            "nutrient_farmland", () -> new AdvancedFarmlandBlock(BlockBehaviour.Properties.of()
                    .strength(0.9f).sound(SoundType.GRAVEL).randomTicks(), 2));
    public static final Supplier<AdvancedFarmlandBlock> MYTHIC_FARMLAND = BLOCKS.register(
            "mythic_farmland", () -> new AdvancedFarmlandBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f).sound(SoundType.GRAVEL).randomTicks(), 3));

    // Additional farming progression; no existing registry IDs or textures replaced.
    public static final Supplier<AdvancedFarmlandBlock> ARCANE_FARMLAND = BLOCKS.register(
            "arcane_farmland", () -> new AdvancedFarmlandBlock(BlockBehaviour.Properties.of()
                    .strength(1.2f).sound(SoundType.GRAVEL).randomTicks(), 4));
    public static final Supplier<AdvancedFarmlandBlock> CELESTIAL_FARMLAND = BLOCKS.register(
            "celestial_farmland", () -> new AdvancedFarmlandBlock(BlockBehaviour.Properties.of()
                    .strength(1.4f).sound(SoundType.GRAVEL).randomTicks(), 5));

    public static final Supplier<IrrigationSprinklerBlock> IRRIGATION_SPRINKLER = BLOCKS.register(
            "irrigation_sprinkler", () -> new IrrigationSprinklerBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.COPPER).noOcclusion()));
    public static final Supplier<IrrigationValveBlock> IRRIGATION_VALVE = BLOCKS.register(
            "irrigation_valve", () -> new IrrigationValveBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f).sound(SoundType.COPPER).noOcclusion()));

    public static final Supplier<IrrigationPipeBlock> IRRIGATION_PIPE=BLOCKS.register("irrigation_pipe",
            ()->new IrrigationPipeBlock(BlockBehaviour.Properties.of().strength(2f).sound(SoundType.COPPER).noOcclusion()));
    public static final Supplier<WaterReservoirBlock> WATER_RESERVOIR=BLOCKS.register("water_reservoir",
            ()->new WaterReservoirBlock(BlockBehaviour.Properties.of().strength(3f).sound(SoundType.COPPER).noOcclusion()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    private AEBlocks() {}
}
