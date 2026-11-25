package com.agrievolution.content;

import com.agrievolution.platform.RegistrationSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class AEBlockEntities {
    public static final RegistrationSet<BlockEntityType<?>> TYPES =
            new RegistrationSet<>(Registries.BLOCK_ENTITY_TYPE);
    public static final Supplier<BlockEntityType<IrrigationControllerBlockEntity>> IRRIGATION_CONTROLLER = TYPES.register(
            "irrigation_controller", () -> BlockEntityType.Builder.of(IrrigationControllerBlockEntity::new, AEBlocks.IRRIGATION_CONTROLLER.get()).build(null));
    public static final Supplier<BlockEntityType<BatchCookingBlockEntity>> BATCH_COOKING_STATION = TYPES.register(
            "batch_cooking_station", () -> BlockEntityType.Builder.of(BatchCookingBlockEntity::new, AEBlocks.BATCH_COOKING_STATION.get()).build(null));
    public static final Supplier<BlockEntityType<IrrigationValveBlockEntity>> IRRIGATION_VALVE = TYPES.register(
            "irrigation_valve", () -> BlockEntityType.Builder.of(IrrigationValveBlockEntity::new, AEBlocks.IRRIGATION_VALVE.get()).build(null));
    public static final Supplier<BlockEntityType<SeedVaultBlockEntity>> SEED_VAULT = TYPES.register(
            "seed_vault", () -> BlockEntityType.Builder.of(SeedVaultBlockEntity::new, AEBlocks.SEED_VAULT.get()).build(null));

    // Keep the old block entity type ID for save compatibility. Every species-specific
    // AgriCropBlock must be listed here, otherwise planting it throws
    // IllegalStateException: Invalid block entity (on BOTH server and client).
    // verifyCropBlockEntityRegistration validates this list against CropSpecies.
    public static final Supplier<BlockEntityType<AECropBlockEntity>> AGRI_CROP = TYPES.register(
            "agri_crop", () -> BlockEntityType.Builder.of(AECropBlockEntity::new,
                    AEBlocks.AGRI_CROP.get(), AEBlocks.CRIMSON_CLOVER_CROP.get(),
                    AEBlocks.AZURE_FLAX_CROP.get(), AEBlocks.VIOLET_STARFLOWER_CROP.get(),
                    AEBlocks.AMBER_MARIGOLD_CROP.get(), AEBlocks.SILVER_THISTLE_CROP.get(),
                    AEBlocks.SUNFIRE_LILY_CROP.get(), AEBlocks.MOON_ORCHID_CROP.get(),
                    AEBlocks.ECLIPSE_LOTUS_CROP.get(), AEBlocks.TWILIGHT_SAGE_CROP.get(),
                    AEBlocks.DIRT_CROP.get(), AEBlocks.WOOD_CROP.get(), AEBlocks.STONE_CROP.get(), AEBlocks.COAL_CROP.get(), AEBlocks.COPPER_CROP.get(), AEBlocks.IRON_CROP.get(), AEBlocks.REDSTONE_CROP.get(), AEBlocks.GOLD_CROP.get(), AEBlocks.LAPIS_CROP.get(), AEBlocks.DIAMOND_CROP.get(), AEBlocks.EMERALD_CROP.get(), AEBlocks.QUARTZ_CROP.get(),
                    AEBlocks.GRAVEL_CROP.get(), AEBlocks.SAND_CROP.get(),
                    AEBlocks.CLAY_CROP.get(), AEBlocks.FLINT_CROP.get(),
                    AEBlocks.GLOWSTONE_CROP.get(), AEBlocks.OBSIDIAN_CROP.get(),
                    AEBlocks.NETHERRACK_CROP.get(), AEBlocks.SOUL_SAND_CROP.get(), AEBlocks.MAGMA_CREAM_CROP.get(), AEBlocks.NETHER_WART_CROP.get(), AEBlocks.BLAZE_ROD_CROP.get(), AEBlocks.END_STONE_CROP.get(), AEBlocks.CHORUS_FRUIT_CROP.get(), AEBlocks.ENDER_PEARL_CROP.get(), AEBlocks.SHULKER_SHELL_CROP.get(), AEBlocks.ANCIENT_DEBRIS_CROP.get(), AEBlocks.NETHERITE_CROP.get(), AEBlocks.TOMATO_CROP.get(), AEBlocks.LETTUCE_CROP.get(), AEBlocks.CUCUMBER_CROP.get(), AEBlocks.ONION_CROP.get(), AEBlocks.GARLIC_CROP.get(), AEBlocks.PEPPER_CROP.get(), AEBlocks.STRAWBERRY_CROP.get(), AEBlocks.RICE_CROP.get(), AEBlocks.CORN_CROP.get(), AEBlocks.PEAS_CROP.get(), AEBlocks.BEANS_CROP.get(), AEBlocks.BROCCOLI_CROP.get(), AEBlocks.CABBAGE_CROP.get(), AEBlocks.RADISH_CROP.get(), AEBlocks.AUBERGINE_CROP.get(), AEBlocks.SWEET_POTATO_CROP.get(), AEBlocks.BASIL_CROP.get(), AEBlocks.MINT_CROP.get(), AEBlocks.ROSEMARY_CROP.get(), AEBlocks.THYME_CROP.get(), AEBlocks.PARSLEY_CROP.get(), AEBlocks.CORIANDER_CROP.get(), AEBlocks.GINGER_CROP.get(), AEBlocks.TURMERIC_CROP.get(), AEBlocks.STRING_CROP.get(), AEBlocks.WOOL_CROP.get(), AEBlocks.LEATHER_CROP.get(), AEBlocks.BONE_CROP.get(), AEBlocks.AMETHYST_CROP.get(), AEBlocks.PRISMARINE_CROP.get(), AEBlocks.GHAST_TEAR_CROP.get(), AEBlocks.BREEZE_ROD_CROP.get(), AEBlocks.FEATHER_CROP.get(), AEBlocks.INK_SAC_CROP.get(), AEBlocks.HONEYCOMB_CROP.get(), AEBlocks.SLIME_BALL_CROP.get(), AEBlocks.GUNPOWDER_CROP.get(), AEBlocks.PRISMARINE_CRYSTALS_CROP.get(), AEBlocks.PHANTOM_MEMBRANE_CROP.get(), AEBlocks.ECHO_SHARD_CROP.get(), AEBlocks.SNOWBALL_CROP.get(), AEBlocks.MOSS_CROP.get(), AEBlocks.CALCITE_CROP.get(), AEBlocks.TERRACOTTA_CROP.get(), AEBlocks.SPIDER_EYE_CROP.get(), AEBlocks.GLOW_INK_SAC_CROP.get(), AEBlocks.CRYING_OBSIDIAN_CROP.get(), AEBlocks.NAUTILUS_SHELL_CROP.get(), AEBlocks.PAPER_CROP.get(), AEBlocks.KELP_CROP.get(), AEBlocks.BAMBOO_CROP.get(), AEBlocks.BLACKSTONE_CROP.get(), AEBlocks.BASALT_CROP.get(), AEBlocks.PURPUR_CROP.get(), AEBlocks.TURTLE_SCUTE_CROP.get(), AEBlocks.SPONGE_CROP.get(), AEBlocks.GRANITE_CROP.get(), AEBlocks.DIORITE_CROP.get(), AEBlocks.ANDESITE_CROP.get(), AEBlocks.MUD_CROP.get(), AEBlocks.TUFF_CROP.get(), AEBlocks.DRIPSTONE_CROP.get(), AEBlocks.DEEPSLATE_CROP.get(), AEBlocks.SOUL_SOIL_CROP.get(), AEBlocks.SEA_LANTERN_CROP.get(), AEBlocks.SHROOMLIGHT_CROP.get(), AEBlocks.END_ROD_CROP.get(), AEBlocks.SCULK_CROP.get(), AEBlocks.RED_SAND_CROP.get(), AEBlocks.SANDSTONE_CROP.get(), AEBlocks.CACTUS_CROP.get(), AEBlocks.VINE_CROP.get(), AEBlocks.PACKED_MUD_CROP.get(), AEBlocks.BRICK_CROP.get(), AEBlocks.ICE_CROP.get(), AEBlocks.PACKED_ICE_CROP.get(), AEBlocks.BLUE_ICE_CROP.get(), AEBlocks.NETHER_BRICK_CROP.get(), AEBlocks.ENDER_EYE_CROP.get(), AEBlocks.CHARCOAL_CROP.get(), AEBlocks.GLASS_CROP.get(), AEBlocks.BROWN_MUSHROOM_CROP.get(), AEBlocks.RED_MUSHROOM_CROP.get(), AEBlocks.MOSSY_COBBLESTONE_CROP.get(), AEBlocks.LILY_PAD_CROP.get(), AEBlocks.DARK_PRISMARINE_CROP.get(), AEBlocks.GILDED_BLACKSTONE_CROP.get(), AEBlocks.SCULK_CATALYST_CROP.get(), AEBlocks.OCHRE_FROGLIGHT_CROP.get(), AEBlocks.SPRUCE_LOG_CROP.get(), AEBlocks.BIRCH_LOG_CROP.get(), AEBlocks.ACACIA_LOG_CROP.get(), AEBlocks.DARK_OAK_LOG_CROP.get(), AEBlocks.MANGROVE_LOG_CROP.get(), AEBlocks.CHERRY_LOG_CROP.get(), AEBlocks.SEA_PICKLE_CROP.get(), AEBlocks.GLOW_LICHEN_CROP.get(), AEBlocks.CRIMSON_STEM_CROP.get(), AEBlocks.WARPED_STEM_CROP.get(), AEBlocks.VERDANT_FROGLIGHT_CROP.get(), AEBlocks.PEARLESCENT_FROGLIGHT_CROP.get(), AEBlocks.SUGAR_CROP.get(), AEBlocks.COCOA_BEANS_CROP.get(), AEBlocks.EGG_CROP.get(), AEBlocks.ROTTEN_FLESH_CROP.get(), AEBlocks.RABBIT_HIDE_CROP.get(), AEBlocks.RABBIT_FOOT_CROP.get(), AEBlocks.ARMADILLO_SCUTE_CROP.get(), AEBlocks.FERMENTED_SPIDER_EYE_CROP.get(), AEBlocks.WITHER_ROSE_CROP.get(), AEBlocks.CHORUS_FLOWER_CROP.get(), AEBlocks.CRIMSON_FUNGUS_CROP.get(), AEBlocks.WARPED_FUNGUS_CROP.get(), AEBlocks.GREEN_DYE_CROP.get(), AEBlocks.RED_DYE_CROP.get(), AEBlocks.YELLOW_DYE_CROP.get(), AEBlocks.BROWN_DYE_CROP.get(), AEBlocks.HONEY_BOTTLE_CROP.get(), AEBlocks.GLOW_BERRIES_CROP.get(), AEBlocks.BLUE_DYE_CROP.get(), AEBlocks.CYAN_DYE_CROP.get(), AEBlocks.FIRE_CHARGE_CROP.get(), AEBlocks.WIND_CHARGE_CROP.get(), AEBlocks.DRAGON_BREATH_CROP.get(), AEBlocks.EXPERIENCE_BOTTLE_CROP.get(), AEBlocks.WHITE_DYE_CROP.get(), AEBlocks.BLACK_DYE_CROP.get(), AEBlocks.ORANGE_DYE_CROP.get(), AEBlocks.PINK_DYE_CROP.get(), AEBlocks.PURPLE_DYE_CROP.get(), AEBlocks.MAGENTA_DYE_CROP.get(), AEBlocks.LIGHT_BLUE_DYE_CROP.get(), AEBlocks.LIME_DYE_CROP.get(), AEBlocks.HEART_OF_THE_SEA_CROP.get(), AEBlocks.NETHER_STAR_CROP.get(), AEBlocks.DRAGON_HEAD_CROP.get(), AEBlocks.HEAVY_CORE_CROP.get(), AEBlocks.STICK_CROP.get(), AEBlocks.DRIED_KELP_CROP.get(), AEBlocks.GRAY_DYE_CROP.get(), AEBlocks.LIGHT_GRAY_DYE_CROP.get(), AEBlocks.SKELETON_SKULL_CROP.get(), AEBlocks.WITHER_SKELETON_SKULL_CROP.get(), AEBlocks.CREEPER_HEAD_CROP.get(), AEBlocks.ZOMBIE_HEAD_CROP.get(), AEBlocks.PUMPKIN_CROP.get(), AEBlocks.BUTTERNUT_SQUASH_CROP.get(), AEBlocks.COURGETTE_CROP.get(), AEBlocks.TURNIP_CROP.get(), AEBlocks.PARSNIP_CROP.get(), AEBlocks.LEEK_CROP.get(), AEBlocks.CELERY_CROP.get(), AEBlocks.ASPARAGUS_CROP.get(), AEBlocks.BLUEBERRY_CROP.get(), AEBlocks.RASPBERRY_CROP.get(), AEBlocks.BLACKBERRY_CROP.get(), AEBlocks.CRANBERRY_CROP.get(), AEBlocks.SPINACH_CROP.get(), AEBlocks.CAULIFLOWER_CROP.get(), AEBlocks.ARTICHOKE_CROP.get(), AEBlocks.CHILLI_CROP.get(), AEBlocks.SPRING_ONION_CROP.get(), AEBlocks.FENNEL_CROP.get(), AEBlocks.DILL_CROP.get(), AEBlocks.PEANUT_CROP.get(), AEBlocks.SESAME_CROP.get(), AEBlocks.OATS_CROP.get(), AEBlocks.GOOSEBERRY_CROP.get(), AEBlocks.BLACKCURRANT_CROP.get(), AEBlocks.KALE_CROP.get(), AEBlocks.PAK_CHOI_CROP.get(), AEBlocks.BRUSSELS_SPROUTS_CROP.get(), AEBlocks.SWEDE_CROP.get(), AEBlocks.CELERIAC_CROP.get(), AEBlocks.LENTIL_CROP.get(), AEBlocks.CHICKPEA_CROP.get(), AEBlocks.BARLEY_CROP.get(), AEBlocks.BUCKWHEAT_CROP.get(), AEBlocks.RHUBARB_CROP.get(), AEBlocks.GRAPE_CROP.get(), AEBlocks.CANTALOUPE_CROP.get(), AEBlocks.JUNGLE_LOG_CROP.get(), AEBlocks.COARSE_DIRT_CROP.get(), AEBlocks.ROOTED_DIRT_CROP.get(), AEBlocks.RED_SANDSTONE_CROP.get(), AEBlocks.PRISMARINE_BRICKS_CROP.get(), AEBlocks.WARPED_WART_BLOCK_CROP.get(), AEBlocks.RED_NETHER_BRICKS_CROP.get(), AEBlocks.SCULK_SENSOR_CROP.get(), AEBlocks.SCULK_SHRIEKER_CROP.get(), AEBlocks.CONDUIT_CROP.get(), AEBlocks.RESPAWN_ANCHOR_CROP.get(), AEBlocks.GRASS_BLOCK_CROP.get(), AEBlocks.PODZOL_CROP.get(), AEBlocks.MYCELIUM_CROP.get(), AEBlocks.DEAD_BUSH_CROP.get(), AEBlocks.MUD_BRICKS_CROP.get(), AEBlocks.DEEPSLATE_BRICKS_CROP.get(), AEBlocks.CALIBRATED_SCULK_SENSOR_CROP.get(), AEBlocks.END_CRYSTAL_CROP.get(), AEBlocks.TOTEM_OF_UNDYING_CROP.get(), AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP.get(), AEBlocks.SUGAR_CANE_CROP.get(), AEBlocks.SWEET_BERRIES_CROP.get(), AEBlocks.SEAGRASS_CROP.get(), AEBlocks.HANGING_ROOTS_CROP.get(), AEBlocks.TORCHFLOWER_CROP.get(), AEBlocks.PITCHER_PLANT_CROP.get(), AEBlocks.SPORE_BLOSSOM_CROP.get(), AEBlocks.BIG_DRIPLEAF_CROP.get(), AEBlocks.TRIAL_KEY_CROP.get(), AEBlocks.OMINOUS_TRIAL_KEY_CROP.get(), AEBlocks.NETHERITE_UPGRADE_SMITHING_TEMPLATE_CROP.get(), AEBlocks.DISC_FRAGMENT_5_CROP.get(), AEBlocks.OAK_SAPLING_CROP.get(), AEBlocks.SPRUCE_SAPLING_CROP.get(), AEBlocks.BIRCH_SAPLING_CROP.get(), AEBlocks.DARK_OAK_SAPLING_CROP.get(), AEBlocks.TUBE_CORAL_CROP.get(), AEBlocks.BRAIN_CORAL_CROP.get(), AEBlocks.BUBBLE_CORAL_CROP.get(), AEBlocks.FIRE_CORAL_CROP.get(), AEBlocks.COAST_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get(), AEBlocks.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get(), AEBlocks.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get(), AEBlocks.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP.get()).build(null));

    // The ticket is transient; the existing filled block state persists normally.
    public static final Supplier<BlockEntityType<IrrigationBasinBlockEntity>> IRRIGATION_BASIN = TYPES.register(
            "irrigation_basin", () -> BlockEntityType.Builder.of(IrrigationBasinBlockEntity::new,
                    AEBlocks.IRRIGATION_BASIN.get()).build(null));

    public static final Supplier<BlockEntityType<SeedBreedingStationBlockEntity>> SEED_BREEDING_STATION = TYPES.register(
            "seed_breeding_station", () -> BlockEntityType.Builder.of(SeedBreedingStationBlockEntity::new,
                    AEBlocks.SEED_BREEDING_STATION.get()).build(null));

    public static final Supplier<BlockEntityType<BotanicalGreenhouseBlockEntity>> BOTANICAL_GREENHOUSE = TYPES.register(
            "botanical_greenhouse", () -> BlockEntityType.Builder.of(BotanicalGreenhouseBlockEntity::new,
                    AEBlocks.BOTANICAL_GREENHOUSE.get()).build(null));

    public static final Supplier<BlockEntityType<VerticalGreenhouseBlockEntity>> VERTICAL_GREENHOUSE = TYPES.register(
            "vertical_greenhouse", () -> BlockEntityType.Builder.of(VerticalGreenhouseBlockEntity::new,
                    AEBlocks.VERTICAL_GREENHOUSE.get()).build(null));

    public static final Supplier<BlockEntityType<BotanicalGeneticsLaboratoryBlockEntity>> BOTANICAL_GENETICS_LABORATORY = TYPES.register(
            "botanical_genetics_laboratory", () -> BlockEntityType.Builder.of(BotanicalGeneticsLaboratoryBlockEntity::new,
                    AEBlocks.BOTANICAL_GENETICS_LABORATORY.get()).build(null));

    public static final Supplier<BlockEntityType<SoilResearchTableBlockEntity>> SOIL_RESEARCH_TABLE = TYPES.register(
            "soil_research_table", () -> BlockEntityType.Builder.of(SoilResearchTableBlockEntity::new,
                    AEBlocks.SOIL_RESEARCH_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<IrrigationSprinklerBlockEntity>> IRRIGATION_SPRINKLER = TYPES.register(
            "irrigation_sprinkler", () -> BlockEntityType.Builder.of(IrrigationSprinklerBlockEntity::new,
                    AEBlocks.IRRIGATION_SPRINKLER.get()).build(null));

    public static final Supplier<BlockEntityType<WaterReservoirBlockEntity>> WATER_RESERVOIR=TYPES.register("water_reservoir",
            ()->BlockEntityType.Builder.of(WaterReservoirBlockEntity::new,AEBlocks.WATER_RESERVOIR.get()).build(null));
    private AEBlockEntities() {}
}

