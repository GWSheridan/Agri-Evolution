package com.agrievolution.content;

import com.agrievolution.platform.RegistrationSet;
import com.agrievolution.platform.ContentRegistration;
import java.util.function.Supplier;
import com.agrievolution.genetics.CropSpecies;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

public final class AEItems {
    public static final RegistrationSet<Item> ITEMS = new RegistrationSet<>(net.minecraft.core.registries.Registries.ITEM);
    public static final Supplier<CompostMulchItem> COMPOST_MULCH = ITEMS.registerItem("compost_mulch", CompostMulchItem::new, new Item.Properties());
    public static final Supplier<FarmLedgerItem> FARM_LEDGER = ITEMS.registerItem("farm_ledger", FarmLedgerItem::new, new Item.Properties().stacksTo(1));
    static { ResourceCropMigration.aliases(ITEMS, "_seed"); }
    public static final Supplier<BlockItem> COMPOST_BIN = ITEMS.registerItem("compost_bin",
            properties -> new BlockItem(AEBlocks.COMPOST_BIN.get(), properties), new Item.Properties());
    public static final Supplier<Item> PULLED_WEEDS = ITEMS.registerItem("pulled_weeds", Item::new, new Item.Properties());
    public static final Supplier<BlockItem> SEED_VAULT = ITEMS.registerItem("seed_vault",
            properties -> new BlockItem(AEBlocks.SEED_VAULT.get(), properties), new Item.Properties());

    public static final Supplier<BlockItem> VERTICAL_GREENHOUSE = ITEMS.registerItem("vertical_greenhouse",
            properties -> new IrrigationFittingItem(AEBlocks.VERTICAL_GREENHOUSE.get(), properties), new Item.Properties());
    public static final Supplier<BlockItem> IRRIGATION_CONTROLLER = ITEMS.registerItem("irrigation_controller",
            properties -> new BlockItem(AEBlocks.IRRIGATION_CONTROLLER.get(), properties), new Item.Properties());
    public static final Supplier<BlockItem> RESOURCE_WORKSHOP = ITEMS.registerItem("resource_workshop",
            properties -> new BlockItem(AEBlocks.RESOURCE_WORKSHOP.get(), properties), new Item.Properties());
    public static final Supplier<Item> SOIL_CONDITIONER = ITEMS.register("soil_conditioner",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> COPPER_COUPLING = ITEMS.register("copper_coupling",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> AMETHYST_LENS = ITEMS.register("amethyst_lens",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> REINFORCED_BRACKET = ITEMS.register("reinforced_bracket",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> NETHER_HEAT_CORE = ITEMS.register("nether_heat_core",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ENDER_RELAY = ITEMS.register("ender_relay",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> WORKSHOP_REFINEMENT_UPGRADE = ITEMS.register("workshop_refinement_upgrade",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> WORKSHOP_MASTERY_UPGRADE = ITEMS.register("workshop_mastery_upgrade",
            () -> new Item(new Item.Properties()));
    public static final Supplier<BlockItem> BATCH_COOKING_STATION = ITEMS.registerItem("batch_cooking_station",
            properties -> new BlockItem(AEBlocks.BATCH_COOKING_STATION.get(), properties), new Item.Properties());
    public static final Supplier<BlockItem> GARDEN_ORDER_BOARD = ITEMS.registerItem("garden_order_board",
            properties -> new BlockItem(AEBlocks.GARDEN_ORDER_BOARD.get(), properties), new Item.Properties());
    public static final Supplier<BlockItem> PRESERVING_STATION = ITEMS.registerItem("preserving_station",
            properties -> new BlockItem(AEBlocks.PRESERVING_STATION.get(), properties), new Item.Properties());
    public static final Supplier<Item> BERRY_JAM = ITEMS.registerItem("berry_jam", Item::new,
            new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.4f)
                            .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).build()));
    public static final Supplier<Item> DILL_PICKLES = ITEMS.registerItem("dill_pickles", Item::new,
            new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)
                    .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(0.5f)
                            .usingConvertsTo(net.minecraft.world.item.Items.GLASS_BOTTLE).build()));
    public static final Supplier<Item> GARDEN_SEASONING = ITEMS.registerItem("garden_seasoning", Item::new,
            new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));

    public static final Supplier<LunchBoxItem> LUNCH_BOX = ITEMS.registerItem("lunch_box", LunchBoxItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<SeedBagItem> SEED_BAG = ITEMS.registerItem("seed_bag",
            SeedBagItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<GardenToolItem> RAKE_WOOD = ITEMS.registerItem("rake_wood",
            props -> ContentRegistration.platform().gardenTool(props, false, 10), new Item.Properties().durability(64));
    public static final Supplier<GardenToolItem> RAKE_IRON = ITEMS.registerItem("rake_iron",
            props -> ContentRegistration.platform().gardenTool(props, false, 20), new Item.Properties().durability(250));
    public static final Supplier<GardenToolItem> TROWEL = ITEMS.registerItem("trowel_empty",
            props -> ContentRegistration.platform().gardenTool(props, true, 10), new Item.Properties().durability(180));

    public static final Supplier<Item> WILD_SEED =
            ITEMS.registerItem("wild_seed", WildSeedItem::new, new Item.Properties());
    public static final Supplier<Item> CRIMSON_CLOVER_SEED =
            ITEMS.registerItem("crimson_clover_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.CRIMSON_CLOVER, AEBlocks.CRIMSON_CLOVER_CROP::get), new Item.Properties());
    public static final Supplier<Item> AZURE_FLAX_SEED =
            ITEMS.registerItem("azure_flax_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.AZURE_FLAX, AEBlocks.AZURE_FLAX_CROP::get), new Item.Properties());
    public static final Supplier<Item> VIOLET_STARFLOWER_SEED =
            ITEMS.registerItem("violet_starflower_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.VIOLET_STARFLOWER, AEBlocks.VIOLET_STARFLOWER_CROP::get), new Item.Properties());

    public static final Supplier<Item> AMBER_MARIGOLD_SEED =
            ITEMS.registerItem("amber_marigold_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.AMBER_MARIGOLD, AEBlocks.AMBER_MARIGOLD_CROP::get), new Item.Properties());
    public static final Supplier<Item> SILVER_THISTLE_SEED =
            ITEMS.registerItem("silver_thistle_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.SILVER_THISTLE, AEBlocks.SILVER_THISTLE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SUNFIRE_LILY_SEED =
            ITEMS.registerItem("sunfire_lily_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.SUNFIRE_LILY, AEBlocks.SUNFIRE_LILY_CROP::get), new Item.Properties());
    public static final Supplier<Item> MOON_ORCHID_SEED =
            ITEMS.registerItem("moon_orchid_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.MOON_ORCHID, AEBlocks.MOON_ORCHID_CROP::get), new Item.Properties());
    public static final Supplier<Item> ECLIPSE_LOTUS_SEED =
            ITEMS.registerItem("eclipse_lotus_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.ECLIPSE_LOTUS, AEBlocks.ECLIPSE_LOTUS_CROP::get), new Item.Properties());
    public static final Supplier<Item> TWILIGHT_SAGE_SEED =
            ITEMS.registerItem("twilight_sage_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.TWILIGHT_SAGE, AEBlocks.TWILIGHT_SAGE_CROP::get), new Item.Properties());

    public static final Supplier<Item> DIRT_SEED =
            ITEMS.registerItem("dirt_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.DIRT, AEBlocks.DIRT_CROP::get), new Item.Properties());
    public static final Supplier<Item> WOOD_SEED =
            ITEMS.registerItem("wood_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.WOOD, AEBlocks.WOOD_CROP::get), new Item.Properties());
    public static final Supplier<Item> STONE_SEED =
            ITEMS.registerItem("stone_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.STONE, AEBlocks.STONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> COAL_SEED =
            ITEMS.registerItem("coal_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.COAL, AEBlocks.COAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> COPPER_SEED =
            ITEMS.registerItem("copper_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.COPPER, AEBlocks.COPPER_CROP::get), new Item.Properties());
    public static final Supplier<Item> IRON_SEED =
            ITEMS.registerItem("iron_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.IRON, AEBlocks.IRON_CROP::get), new Item.Properties());
    public static final Supplier<Item> REDSTONE_SEED =
            ITEMS.registerItem("redstone_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.REDSTONE, AEBlocks.REDSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GOLD_SEED =
            ITEMS.registerItem("gold_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.GOLD, AEBlocks.GOLD_CROP::get), new Item.Properties());
    public static final Supplier<Item> LAPIS_SEED =
            ITEMS.registerItem("lapis_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.LAPIS, AEBlocks.LAPIS_CROP::get), new Item.Properties());
    public static final Supplier<Item> DIAMOND_SEED =
            ITEMS.registerItem("diamond_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.DIAMOND, AEBlocks.DIAMOND_CROP::get), new Item.Properties());
    public static final Supplier<Item> EMERALD_SEED =
            ITEMS.registerItem("emerald_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.EMERALD, AEBlocks.EMERALD_CROP::get), new Item.Properties());
    public static final Supplier<Item> QUARTZ_SEED =
            ITEMS.registerItem("quartz_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.QUARTZ, AEBlocks.QUARTZ_CROP::get), new Item.Properties());

    public static final Supplier<Item> GRAVEL_SEED =
            ITEMS.registerItem("gravel_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.GRAVEL, AEBlocks.GRAVEL_CROP::get), new Item.Properties());
    public static final Supplier<Item> SAND_SEED =
            ITEMS.registerItem("sand_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.SAND, AEBlocks.SAND_CROP::get), new Item.Properties());
    public static final Supplier<Item> CLAY_SEED =
            ITEMS.registerItem("clay_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.CLAY, AEBlocks.CLAY_CROP::get), new Item.Properties());
    public static final Supplier<Item> FLINT_SEED =
            ITEMS.registerItem("flint_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.FLINT, AEBlocks.FLINT_CROP::get), new Item.Properties());
    public static final Supplier<Item> GLOWSTONE_SEED =
            ITEMS.registerItem("glowstone_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.GLOWSTONE, AEBlocks.GLOWSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> OBSIDIAN_SEED =
            ITEMS.registerItem("obsidian_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.OBSIDIAN, AEBlocks.OBSIDIAN_CROP::get), new Item.Properties());

    public static final Supplier<Item> NETHERRACK_SEED =
            ITEMS.registerItem("netherrack_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.NETHERRACK, AEBlocks.NETHERRACK_CROP::get), new Item.Properties());
    public static final Supplier<Item> SOUL_SAND_SEED =
            ITEMS.registerItem("soul_sand_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.SOUL_SAND, AEBlocks.SOUL_SAND_CROP::get), new Item.Properties());
    public static final Supplier<Item> MAGMA_CREAM_SEED =
            ITEMS.registerItem("magma_cream_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.MAGMA_CREAM, AEBlocks.MAGMA_CREAM_CROP::get), new Item.Properties());
    public static final Supplier<Item> NETHER_WART_SEED =
            ITEMS.registerItem("nether_wart_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.NETHER_WART, AEBlocks.NETHER_WART_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLAZE_ROD_SEED =
            ITEMS.registerItem("blaze_rod_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.BLAZE_ROD, AEBlocks.BLAZE_ROD_CROP::get), new Item.Properties());
    public static final Supplier<Item> END_STONE_SEED =
            ITEMS.registerItem("end_stone_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.END_STONE, AEBlocks.END_STONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CHORUS_FRUIT_SEED =
            ITEMS.registerItem("chorus_fruit_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.CHORUS_FRUIT, AEBlocks.CHORUS_FRUIT_CROP::get), new Item.Properties());
    public static final Supplier<Item> ENDER_PEARL_SEED =
            ITEMS.registerItem("ender_pearl_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.ENDER_PEARL, AEBlocks.ENDER_PEARL_CROP::get), new Item.Properties());
    public static final Supplier<Item> SHULKER_SHELL_SEED =
            ITEMS.registerItem("shulker_shell_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.SHULKER_SHELL, AEBlocks.SHULKER_SHELL_CROP::get), new Item.Properties());
    public static final Supplier<Item> ANCIENT_DEBRIS_SEED =
            ITEMS.registerItem("ancient_debris_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.ANCIENT_DEBRIS, AEBlocks.ANCIENT_DEBRIS_CROP::get), new Item.Properties());
    public static final Supplier<Item> NETHERITE_SEED =
            ITEMS.registerItem("netherite_seed", props -> new SpeciesSeedItem(props,
                    CropSpecies.NETHERITE, AEBlocks.NETHERITE_CROP::get), new Item.Properties());

    public static final Supplier<Item> TOMATO = ITEMS.registerItem("tomato", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> TOMATO_SEED = ITEMS.registerItem("tomato_seed", props -> new SpeciesSeedItem(props, CropSpecies.TOMATO, AEBlocks.TOMATO_CROP::get), new Item.Properties());
    public static final Supplier<Item> LETTUCE = ITEMS.registerItem("lettuce", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> LETTUCE_SEED = ITEMS.registerItem("lettuce_seed", props -> new SpeciesSeedItem(props, CropSpecies.LETTUCE, AEBlocks.LETTUCE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CUCUMBER = ITEMS.registerItem("cucumber", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CUCUMBER_SEED = ITEMS.registerItem("cucumber_seed", props -> new SpeciesSeedItem(props, CropSpecies.CUCUMBER, AEBlocks.CUCUMBER_CROP::get), new Item.Properties());
    public static final Supplier<Item> ONION = ITEMS.registerItem("onion", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> ONION_SEED = ITEMS.registerItem("onion_seed", props -> new SpeciesSeedItem(props, CropSpecies.ONION, AEBlocks.ONION_CROP::get), new Item.Properties());
    public static final Supplier<Item> GARLIC = ITEMS.registerItem("garlic", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> GARLIC_SEED = ITEMS.registerItem("garlic_seed", props -> new SpeciesSeedItem(props, CropSpecies.GARLIC, AEBlocks.GARLIC_CROP::get), new Item.Properties());
    public static final Supplier<Item> PEPPER = ITEMS.registerItem("pepper", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PEPPER_SEED = ITEMS.registerItem("pepper_seed", props -> new SpeciesSeedItem(props, CropSpecies.PEPPER, AEBlocks.PEPPER_CROP::get), new Item.Properties());
    public static final Supplier<Item> STRAWBERRY = ITEMS.registerItem("strawberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> STRAWBERRY_SEED = ITEMS.registerItem("strawberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.STRAWBERRY, AEBlocks.STRAWBERRY_CROP::get), new Item.Properties());
    public static final Supplier<Item> RICE = ITEMS.registerItem("rice", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> RICE_SEED = ITEMS.registerItem("rice_seed", props -> new SpeciesSeedItem(props, CropSpecies.RICE, AEBlocks.RICE_CROP::get), new Item.Properties());

    public static final Supplier<Item> CORN = ITEMS.registerItem("corn", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CORN_SEED = ITEMS.registerItem("corn_seed", props -> new SpeciesSeedItem(props, CropSpecies.CORN, AEBlocks.CORN_CROP::get), new Item.Properties());
    public static final Supplier<Item> PEAS = ITEMS.registerItem("peas", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PEAS_SEED = ITEMS.registerItem("peas_seed", props -> new SpeciesSeedItem(props, CropSpecies.PEAS, AEBlocks.PEAS_CROP::get), new Item.Properties());
    public static final Supplier<Item> BEANS = ITEMS.registerItem("beans", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BEANS_SEED = ITEMS.registerItem("beans_seed", props -> new SpeciesSeedItem(props, CropSpecies.BEANS, AEBlocks.BEANS_CROP::get), new Item.Properties());
    public static final Supplier<Item> BROCCOLI = ITEMS.registerItem("broccoli", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BROCCOLI_SEED = ITEMS.registerItem("broccoli_seed", props -> new SpeciesSeedItem(props, CropSpecies.BROCCOLI, AEBlocks.BROCCOLI_CROP::get), new Item.Properties());
    public static final Supplier<Item> CABBAGE = ITEMS.registerItem("cabbage", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CABBAGE_SEED = ITEMS.registerItem("cabbage_seed", props -> new SpeciesSeedItem(props, CropSpecies.CABBAGE, AEBlocks.CABBAGE_CROP::get), new Item.Properties());
    public static final Supplier<Item> RADISH = ITEMS.registerItem("radish", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> RADISH_SEED = ITEMS.registerItem("radish_seed", props -> new SpeciesSeedItem(props, CropSpecies.RADISH, AEBlocks.RADISH_CROP::get), new Item.Properties());
    public static final Supplier<Item> AUBERGINE = ITEMS.registerItem("aubergine", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> AUBERGINE_SEED = ITEMS.registerItem("aubergine_seed", props -> new SpeciesSeedItem(props, CropSpecies.AUBERGINE, AEBlocks.AUBERGINE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SWEET_POTATO = ITEMS.registerItem("sweet_potato", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> SWEET_POTATO_SEED = ITEMS.registerItem("sweet_potato_seed", props -> new SpeciesSeedItem(props, CropSpecies.SWEET_POTATO, AEBlocks.SWEET_POTATO_CROP::get), new Item.Properties());

    public static final Supplier<Item> BASIL = ITEMS.registerItem("basil", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BASIL_SEED = ITEMS.registerItem("basil_seed", props -> new SpeciesSeedItem(props, CropSpecies.BASIL, AEBlocks.BASIL_CROP::get), new Item.Properties());
    public static final Supplier<Item> MINT = ITEMS.registerItem("mint", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> MINT_SEED = ITEMS.registerItem("mint_seed", props -> new SpeciesSeedItem(props, CropSpecies.MINT, AEBlocks.MINT_CROP::get), new Item.Properties());
    public static final Supplier<Item> ROSEMARY = ITEMS.registerItem("rosemary", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> ROSEMARY_SEED = ITEMS.registerItem("rosemary_seed", props -> new SpeciesSeedItem(props, CropSpecies.ROSEMARY, AEBlocks.ROSEMARY_CROP::get), new Item.Properties());
    public static final Supplier<Item> THYME = ITEMS.registerItem("thyme", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> THYME_SEED = ITEMS.registerItem("thyme_seed", props -> new SpeciesSeedItem(props, CropSpecies.THYME, AEBlocks.THYME_CROP::get), new Item.Properties());
    public static final Supplier<Item> PARSLEY = ITEMS.registerItem("parsley", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PARSLEY_SEED = ITEMS.registerItem("parsley_seed", props -> new SpeciesSeedItem(props, CropSpecies.PARSLEY, AEBlocks.PARSLEY_CROP::get), new Item.Properties());
    public static final Supplier<Item> CORIANDER = ITEMS.registerItem("coriander", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CORIANDER_SEED = ITEMS.registerItem("coriander_seed", props -> new SpeciesSeedItem(props, CropSpecies.CORIANDER, AEBlocks.CORIANDER_CROP::get), new Item.Properties());
    public static final Supplier<Item> GINGER = ITEMS.registerItem("ginger", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> GINGER_SEED = ITEMS.registerItem("ginger_seed", props -> new SpeciesSeedItem(props, CropSpecies.GINGER, AEBlocks.GINGER_CROP::get), new Item.Properties());
    public static final Supplier<Item> TURMERIC = ITEMS.registerItem("turmeric", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> TURMERIC_SEED = ITEMS.registerItem("turmeric_seed", props -> new SpeciesSeedItem(props, CropSpecies.TURMERIC, AEBlocks.TURMERIC_CROP::get), new Item.Properties());

    public static final Supplier<Item> STRING_SEED = ITEMS.registerItem("string_seed", props -> new SpeciesSeedItem(props, CropSpecies.STRING, AEBlocks.STRING_CROP::get), new Item.Properties());
    public static final Supplier<Item> WOOL_SEED = ITEMS.registerItem("wool_seed", props -> new SpeciesSeedItem(props, CropSpecies.WOOL, AEBlocks.WOOL_CROP::get), new Item.Properties());
    public static final Supplier<Item> LEATHER_SEED = ITEMS.registerItem("leather_seed", props -> new SpeciesSeedItem(props, CropSpecies.LEATHER, AEBlocks.LEATHER_CROP::get), new Item.Properties());
    public static final Supplier<Item> BONE_SEED = ITEMS.registerItem("bone_seed", props -> new SpeciesSeedItem(props, CropSpecies.BONE, AEBlocks.BONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> AMETHYST_SEED = ITEMS.registerItem("amethyst_seed", props -> new SpeciesSeedItem(props, CropSpecies.AMETHYST, AEBlocks.AMETHYST_CROP::get), new Item.Properties());
    public static final Supplier<Item> PRISMARINE_SEED = ITEMS.registerItem("prismarine_seed", props -> new SpeciesSeedItem(props, CropSpecies.PRISMARINE, AEBlocks.PRISMARINE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GHAST_TEAR_SEED = ITEMS.registerItem("ghast_tear_seed", props -> new SpeciesSeedItem(props, CropSpecies.GHAST_TEAR, AEBlocks.GHAST_TEAR_CROP::get), new Item.Properties());
    public static final Supplier<Item> BREEZE_ROD_SEED = ITEMS.registerItem("breeze_rod_seed", props -> new SpeciesSeedItem(props, CropSpecies.BREEZE_ROD, AEBlocks.BREEZE_ROD_CROP::get), new Item.Properties());

    public static final Supplier<Item> FEATHER_SEED = ITEMS.registerItem("feather_seed", props -> new SpeciesSeedItem(props, CropSpecies.FEATHER, AEBlocks.FEATHER_CROP::get), new Item.Properties());
    public static final Supplier<Item> INK_SAC_SEED = ITEMS.registerItem("ink_sac_seed", props -> new SpeciesSeedItem(props, CropSpecies.INK_SAC, AEBlocks.INK_SAC_CROP::get), new Item.Properties());
    public static final Supplier<Item> HONEYCOMB_SEED = ITEMS.registerItem("honeycomb_seed", props -> new SpeciesSeedItem(props, CropSpecies.HONEYCOMB, AEBlocks.HONEYCOMB_CROP::get), new Item.Properties());
    public static final Supplier<Item> SLIME_BALL_SEED = ITEMS.registerItem("slime_ball_seed", props -> new SpeciesSeedItem(props, CropSpecies.SLIME_BALL, AEBlocks.SLIME_BALL_CROP::get), new Item.Properties());
    public static final Supplier<Item> GUNPOWDER_SEED = ITEMS.registerItem("gunpowder_seed", props -> new SpeciesSeedItem(props, CropSpecies.GUNPOWDER, AEBlocks.GUNPOWDER_CROP::get), new Item.Properties());
    public static final Supplier<Item> PRISMARINE_CRYSTALS_SEED = ITEMS.registerItem("prismarine_crystals_seed", props -> new SpeciesSeedItem(props, CropSpecies.PRISMARINE_CRYSTALS, AEBlocks.PRISMARINE_CRYSTALS_CROP::get), new Item.Properties());
    public static final Supplier<Item> PHANTOM_MEMBRANE_SEED = ITEMS.registerItem("phantom_membrane_seed", props -> new SpeciesSeedItem(props, CropSpecies.PHANTOM_MEMBRANE, AEBlocks.PHANTOM_MEMBRANE_CROP::get), new Item.Properties());
    public static final Supplier<Item> ECHO_SHARD_SEED = ITEMS.registerItem("echo_shard_seed", props -> new SpeciesSeedItem(props, CropSpecies.ECHO_SHARD, AEBlocks.ECHO_SHARD_CROP::get), new Item.Properties());

    public static final Supplier<Item> SNOWBALL_SEED = ITEMS.registerItem("snowball_seed", props -> new SpeciesSeedItem(props, CropSpecies.SNOWBALL, AEBlocks.SNOWBALL_CROP::get), new Item.Properties());
    public static final Supplier<Item> MOSS_SEED = ITEMS.registerItem("moss_seed", props -> new SpeciesSeedItem(props, CropSpecies.MOSS, AEBlocks.MOSS_CROP::get), new Item.Properties());
    public static final Supplier<Item> CALCITE_SEED = ITEMS.registerItem("calcite_seed", props -> new SpeciesSeedItem(props, CropSpecies.CALCITE, AEBlocks.CALCITE_CROP::get), new Item.Properties());
    public static final Supplier<Item> TERRACOTTA_SEED = ITEMS.registerItem("terracotta_seed", props -> new SpeciesSeedItem(props, CropSpecies.TERRACOTTA, AEBlocks.TERRACOTTA_CROP::get), new Item.Properties());
    public static final Supplier<Item> SPIDER_EYE_SEED = ITEMS.registerItem("spider_eye_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPIDER_EYE, AEBlocks.SPIDER_EYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GLOW_INK_SAC_SEED = ITEMS.registerItem("glow_ink_sac_seed", props -> new SpeciesSeedItem(props, CropSpecies.GLOW_INK_SAC, AEBlocks.GLOW_INK_SAC_CROP::get), new Item.Properties());
    public static final Supplier<Item> CRYING_OBSIDIAN_SEED = ITEMS.registerItem("crying_obsidian_seed", props -> new SpeciesSeedItem(props, CropSpecies.CRYING_OBSIDIAN, AEBlocks.CRYING_OBSIDIAN_CROP::get), new Item.Properties());
    public static final Supplier<Item> NAUTILUS_SHELL_SEED = ITEMS.registerItem("nautilus_shell_seed", props -> new SpeciesSeedItem(props, CropSpecies.NAUTILUS_SHELL, AEBlocks.NAUTILUS_SHELL_CROP::get), new Item.Properties());

    public static final Supplier<Item> PAPER_SEED = ITEMS.registerItem("paper_seed", props -> new SpeciesSeedItem(props, CropSpecies.PAPER, AEBlocks.PAPER_CROP::get), new Item.Properties());
    public static final Supplier<Item> KELP_SEED = ITEMS.registerItem("kelp_seed", props -> new SpeciesSeedItem(props, CropSpecies.KELP, AEBlocks.KELP_CROP::get), new Item.Properties());
    public static final Supplier<Item> BAMBOO_SEED = ITEMS.registerItem("bamboo_seed", props -> new SpeciesSeedItem(props, CropSpecies.BAMBOO, AEBlocks.BAMBOO_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLACKSTONE_SEED = ITEMS.registerItem("blackstone_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLACKSTONE, AEBlocks.BLACKSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> BASALT_SEED = ITEMS.registerItem("basalt_seed", props -> new SpeciesSeedItem(props, CropSpecies.BASALT, AEBlocks.BASALT_CROP::get), new Item.Properties());
    public static final Supplier<Item> PURPUR_SEED = ITEMS.registerItem("purpur_seed", props -> new SpeciesSeedItem(props, CropSpecies.PURPUR, AEBlocks.PURPUR_CROP::get), new Item.Properties());
    public static final Supplier<Item> TURTLE_SCUTE_SEED = ITEMS.registerItem("turtle_scute_seed", props -> new SpeciesSeedItem(props, CropSpecies.TURTLE_SCUTE, AEBlocks.TURTLE_SCUTE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SPONGE_SEED = ITEMS.registerItem("sponge_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPONGE, AEBlocks.SPONGE_CROP::get), new Item.Properties());

    public static final Supplier<Item> GRANITE_SEED = ITEMS.registerItem("granite_seed", props -> new SpeciesSeedItem(props, CropSpecies.GRANITE, AEBlocks.GRANITE_CROP::get), new Item.Properties());
    public static final Supplier<Item> DIORITE_SEED = ITEMS.registerItem("diorite_seed", props -> new SpeciesSeedItem(props, CropSpecies.DIORITE, AEBlocks.DIORITE_CROP::get), new Item.Properties());
    public static final Supplier<Item> ANDESITE_SEED = ITEMS.registerItem("andesite_seed", props -> new SpeciesSeedItem(props, CropSpecies.ANDESITE, AEBlocks.ANDESITE_CROP::get), new Item.Properties());
    public static final Supplier<Item> MUD_SEED = ITEMS.registerItem("mud_seed", props -> new SpeciesSeedItem(props, CropSpecies.MUD, AEBlocks.MUD_CROP::get), new Item.Properties());
    public static final Supplier<Item> TUFF_SEED = ITEMS.registerItem("tuff_seed", props -> new SpeciesSeedItem(props, CropSpecies.TUFF, AEBlocks.TUFF_CROP::get), new Item.Properties());
    public static final Supplier<Item> DRIPSTONE_SEED = ITEMS.registerItem("dripstone_seed", props -> new SpeciesSeedItem(props, CropSpecies.DRIPSTONE, AEBlocks.DRIPSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> DEEPSLATE_SEED = ITEMS.registerItem("deepslate_seed", props -> new SpeciesSeedItem(props, CropSpecies.DEEPSLATE, AEBlocks.DEEPSLATE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SOUL_SOIL_SEED = ITEMS.registerItem("soul_soil_seed", props -> new SpeciesSeedItem(props, CropSpecies.SOUL_SOIL, AEBlocks.SOUL_SOIL_CROP::get), new Item.Properties());
    public static final Supplier<Item> SEA_LANTERN_SEED = ITEMS.registerItem("sea_lantern_seed", props -> new SpeciesSeedItem(props, CropSpecies.SEA_LANTERN, AEBlocks.SEA_LANTERN_CROP::get), new Item.Properties());
    public static final Supplier<Item> SHROOMLIGHT_SEED = ITEMS.registerItem("shroomlight_seed", props -> new SpeciesSeedItem(props, CropSpecies.SHROOMLIGHT, AEBlocks.SHROOMLIGHT_CROP::get), new Item.Properties());
    public static final Supplier<Item> END_ROD_SEED = ITEMS.registerItem("end_rod_seed", props -> new SpeciesSeedItem(props, CropSpecies.END_ROD, AEBlocks.END_ROD_CROP::get), new Item.Properties());
    public static final Supplier<Item> SCULK_SEED = ITEMS.registerItem("sculk_seed", props -> new SpeciesSeedItem(props, CropSpecies.SCULK, AEBlocks.SCULK_CROP::get), new Item.Properties());

    public static final Supplier<Item> RED_SAND_SEED = ITEMS.registerItem("red_sand_seed", props -> new SpeciesSeedItem(props, CropSpecies.RED_SAND, AEBlocks.RED_SAND_CROP::get), new Item.Properties());
    public static final Supplier<Item> SANDSTONE_SEED = ITEMS.registerItem("sandstone_seed", props -> new SpeciesSeedItem(props, CropSpecies.SANDSTONE, AEBlocks.SANDSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CACTUS_SEED = ITEMS.registerItem("cactus_seed", props -> new SpeciesSeedItem(props, CropSpecies.CACTUS, AEBlocks.CACTUS_CROP::get), new Item.Properties());
    public static final Supplier<Item> VINE_SEED = ITEMS.registerItem("vine_seed", props -> new SpeciesSeedItem(props, CropSpecies.VINE, AEBlocks.VINE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PACKED_MUD_SEED = ITEMS.registerItem("packed_mud_seed", props -> new SpeciesSeedItem(props, CropSpecies.PACKED_MUD, AEBlocks.PACKED_MUD_CROP::get), new Item.Properties());
    public static final Supplier<Item> BRICK_SEED = ITEMS.registerItem("brick_seed", props -> new SpeciesSeedItem(props, CropSpecies.BRICK, AEBlocks.BRICK_CROP::get), new Item.Properties());
    public static final Supplier<Item> ICE_SEED = ITEMS.registerItem("ice_seed", props -> new SpeciesSeedItem(props, CropSpecies.ICE, AEBlocks.ICE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PACKED_ICE_SEED = ITEMS.registerItem("packed_ice_seed", props -> new SpeciesSeedItem(props, CropSpecies.PACKED_ICE, AEBlocks.PACKED_ICE_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLUE_ICE_SEED = ITEMS.registerItem("blue_ice_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLUE_ICE, AEBlocks.BLUE_ICE_CROP::get), new Item.Properties());
    public static final Supplier<Item> NETHER_BRICK_SEED = ITEMS.registerItem("nether_brick_seed", props -> new SpeciesSeedItem(props, CropSpecies.NETHER_BRICK, AEBlocks.NETHER_BRICK_CROP::get), new Item.Properties());
    public static final Supplier<Item> ENDER_EYE_SEED = ITEMS.registerItem("ender_eye_seed", props -> new SpeciesSeedItem(props, CropSpecies.ENDER_EYE, AEBlocks.ENDER_EYE_CROP::get), new Item.Properties());

    public static final Supplier<Item> CHARCOAL_SEED = ITEMS.registerItem("charcoal_seed", props -> new SpeciesSeedItem(props, CropSpecies.CHARCOAL, AEBlocks.CHARCOAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> GLASS_SEED = ITEMS.registerItem("glass_seed", props -> new SpeciesSeedItem(props, CropSpecies.GLASS, AEBlocks.GLASS_CROP::get), new Item.Properties());
    public static final Supplier<Item> BROWN_MUSHROOM_SEED = ITEMS.registerItem("brown_mushroom_seed", props -> new SpeciesSeedItem(props, CropSpecies.BROWN_MUSHROOM, AEBlocks.BROWN_MUSHROOM_CROP::get), new Item.Properties());
    public static final Supplier<Item> RED_MUSHROOM_SEED = ITEMS.registerItem("red_mushroom_seed", props -> new SpeciesSeedItem(props, CropSpecies.RED_MUSHROOM, AEBlocks.RED_MUSHROOM_CROP::get), new Item.Properties());
    public static final Supplier<Item> MOSSY_COBBLESTONE_SEED = ITEMS.registerItem("mossy_cobblestone_seed", props -> new SpeciesSeedItem(props, CropSpecies.MOSSY_COBBLESTONE, AEBlocks.MOSSY_COBBLESTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> LILY_PAD_SEED = ITEMS.registerItem("lily_pad_seed", props -> new SpeciesSeedItem(props, CropSpecies.LILY_PAD, AEBlocks.LILY_PAD_CROP::get), new Item.Properties());
    public static final Supplier<Item> DARK_PRISMARINE_SEED = ITEMS.registerItem("dark_prismarine_seed", props -> new SpeciesSeedItem(props, CropSpecies.DARK_PRISMARINE, AEBlocks.DARK_PRISMARINE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GILDED_BLACKSTONE_SEED = ITEMS.registerItem("gilded_blackstone_seed", props -> new SpeciesSeedItem(props, CropSpecies.GILDED_BLACKSTONE, AEBlocks.GILDED_BLACKSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SCULK_CATALYST_SEED = ITEMS.registerItem("sculk_catalyst_seed", props -> new SpeciesSeedItem(props, CropSpecies.SCULK_CATALYST, AEBlocks.SCULK_CATALYST_CROP::get), new Item.Properties());
    public static final Supplier<Item> OCHRE_FROGLIGHT_SEED = ITEMS.registerItem("ochre_froglight_seed", props -> new SpeciesSeedItem(props, CropSpecies.OCHRE_FROGLIGHT, AEBlocks.OCHRE_FROGLIGHT_CROP::get), new Item.Properties());

    public static final Supplier<Item> SPRUCE_LOG_SEED = ITEMS.registerItem("spruce_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPRUCE_LOG, AEBlocks.SPRUCE_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> BIRCH_LOG_SEED = ITEMS.registerItem("birch_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.BIRCH_LOG, AEBlocks.BIRCH_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> ACACIA_LOG_SEED = ITEMS.registerItem("acacia_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.ACACIA_LOG, AEBlocks.ACACIA_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> DARK_OAK_LOG_SEED = ITEMS.registerItem("dark_oak_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.DARK_OAK_LOG, AEBlocks.DARK_OAK_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> MANGROVE_LOG_SEED = ITEMS.registerItem("mangrove_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.MANGROVE_LOG, AEBlocks.MANGROVE_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> CHERRY_LOG_SEED = ITEMS.registerItem("cherry_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.CHERRY_LOG, AEBlocks.CHERRY_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> SEA_PICKLE_SEED = ITEMS.registerItem("sea_pickle_seed", props -> new SpeciesSeedItem(props, CropSpecies.SEA_PICKLE, AEBlocks.SEA_PICKLE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GLOW_LICHEN_SEED = ITEMS.registerItem("glow_lichen_seed", props -> new SpeciesSeedItem(props, CropSpecies.GLOW_LICHEN, AEBlocks.GLOW_LICHEN_CROP::get), new Item.Properties());
    public static final Supplier<Item> CRIMSON_STEM_SEED = ITEMS.registerItem("crimson_stem_seed", props -> new SpeciesSeedItem(props, CropSpecies.CRIMSON_STEM, AEBlocks.CRIMSON_STEM_CROP::get), new Item.Properties());
    public static final Supplier<Item> WARPED_STEM_SEED = ITEMS.registerItem("warped_stem_seed", props -> new SpeciesSeedItem(props, CropSpecies.WARPED_STEM, AEBlocks.WARPED_STEM_CROP::get), new Item.Properties());
    public static final Supplier<Item> VERDANT_FROGLIGHT_SEED = ITEMS.registerItem("verdant_froglight_seed", props -> new SpeciesSeedItem(props, CropSpecies.VERDANT_FROGLIGHT, AEBlocks.VERDANT_FROGLIGHT_CROP::get), new Item.Properties());
    public static final Supplier<Item> PEARLESCENT_FROGLIGHT_SEED = ITEMS.registerItem("pearlescent_froglight_seed", props -> new SpeciesSeedItem(props, CropSpecies.PEARLESCENT_FROGLIGHT, AEBlocks.PEARLESCENT_FROGLIGHT_CROP::get), new Item.Properties());

    public static final Supplier<Item> SUGAR_SEED = ITEMS.registerItem("sugar_seed", props -> new SpeciesSeedItem(props, CropSpecies.SUGAR, AEBlocks.SUGAR_CROP::get), new Item.Properties());
    public static final Supplier<Item> COCOA_BEANS_SEED = ITEMS.registerItem("cocoa_beans_seed", props -> new SpeciesSeedItem(props, CropSpecies.COCOA_BEANS, AEBlocks.COCOA_BEANS_CROP::get), new Item.Properties());
    public static final Supplier<Item> EGG_SEED = ITEMS.registerItem("egg_seed", props -> new SpeciesSeedItem(props, CropSpecies.EGG, AEBlocks.EGG_CROP::get), new Item.Properties());
    public static final Supplier<Item> ROTTEN_FLESH_SEED = ITEMS.registerItem("rotten_flesh_seed", props -> new SpeciesSeedItem(props, CropSpecies.ROTTEN_FLESH, AEBlocks.ROTTEN_FLESH_CROP::get), new Item.Properties());
    public static final Supplier<Item> RABBIT_HIDE_SEED = ITEMS.registerItem("rabbit_hide_seed", props -> new SpeciesSeedItem(props, CropSpecies.RABBIT_HIDE, AEBlocks.RABBIT_HIDE_CROP::get), new Item.Properties());
    public static final Supplier<Item> RABBIT_FOOT_SEED = ITEMS.registerItem("rabbit_foot_seed", props -> new SpeciesSeedItem(props, CropSpecies.RABBIT_FOOT, AEBlocks.RABBIT_FOOT_CROP::get), new Item.Properties());
    public static final Supplier<Item> ARMADILLO_SCUTE_SEED = ITEMS.registerItem("armadillo_scute_seed", props -> new SpeciesSeedItem(props, CropSpecies.ARMADILLO_SCUTE, AEBlocks.ARMADILLO_SCUTE_CROP::get), new Item.Properties());
    public static final Supplier<Item> FERMENTED_SPIDER_EYE_SEED = ITEMS.registerItem("fermented_spider_eye_seed", props -> new SpeciesSeedItem(props, CropSpecies.FERMENTED_SPIDER_EYE, AEBlocks.FERMENTED_SPIDER_EYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> WITHER_ROSE_SEED = ITEMS.registerItem("wither_rose_seed", props -> new SpeciesSeedItem(props, CropSpecies.WITHER_ROSE, AEBlocks.WITHER_ROSE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CHORUS_FLOWER_SEED = ITEMS.registerItem("chorus_flower_seed", props -> new SpeciesSeedItem(props, CropSpecies.CHORUS_FLOWER, AEBlocks.CHORUS_FLOWER_CROP::get), new Item.Properties());
    public static final Supplier<Item> CRIMSON_FUNGUS_SEED = ITEMS.registerItem("crimson_fungus_seed", props -> new SpeciesSeedItem(props, CropSpecies.CRIMSON_FUNGUS, AEBlocks.CRIMSON_FUNGUS_CROP::get), new Item.Properties());
    public static final Supplier<Item> WARPED_FUNGUS_SEED = ITEMS.registerItem("warped_fungus_seed", props -> new SpeciesSeedItem(props, CropSpecies.WARPED_FUNGUS, AEBlocks.WARPED_FUNGUS_CROP::get), new Item.Properties());

    public static final Supplier<Item> GREEN_DYE_SEED = ITEMS.registerItem("green_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.GREEN_DYE, AEBlocks.GREEN_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> RED_DYE_SEED = ITEMS.registerItem("red_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.RED_DYE, AEBlocks.RED_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> YELLOW_DYE_SEED = ITEMS.registerItem("yellow_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.YELLOW_DYE, AEBlocks.YELLOW_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> BROWN_DYE_SEED = ITEMS.registerItem("brown_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.BROWN_DYE, AEBlocks.BROWN_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> HONEY_BOTTLE_SEED = ITEMS.registerItem("honey_bottle_seed", props -> new SpeciesSeedItem(props, CropSpecies.HONEY_BOTTLE, AEBlocks.HONEY_BOTTLE_CROP::get), new Item.Properties());
    public static final Supplier<Item> GLOW_BERRIES_SEED = ITEMS.registerItem("glow_berries_seed", props -> new SpeciesSeedItem(props, CropSpecies.GLOW_BERRIES, AEBlocks.GLOW_BERRIES_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLUE_DYE_SEED = ITEMS.registerItem("blue_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLUE_DYE, AEBlocks.BLUE_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CYAN_DYE_SEED = ITEMS.registerItem("cyan_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.CYAN_DYE, AEBlocks.CYAN_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> FIRE_CHARGE_SEED = ITEMS.registerItem("fire_charge_seed", props -> new SpeciesSeedItem(props, CropSpecies.FIRE_CHARGE, AEBlocks.FIRE_CHARGE_CROP::get), new Item.Properties());
    public static final Supplier<Item> WIND_CHARGE_SEED = ITEMS.registerItem("wind_charge_seed", props -> new SpeciesSeedItem(props, CropSpecies.WIND_CHARGE, AEBlocks.WIND_CHARGE_CROP::get), new Item.Properties());
    public static final Supplier<Item> DRAGON_BREATH_SEED = ITEMS.registerItem("dragon_breath_seed", props -> new SpeciesSeedItem(props, CropSpecies.DRAGON_BREATH, AEBlocks.DRAGON_BREATH_CROP::get), new Item.Properties());
    public static final Supplier<Item> EXPERIENCE_BOTTLE_SEED = ITEMS.registerItem("experience_bottle_seed", props -> new SpeciesSeedItem(props, CropSpecies.EXPERIENCE_BOTTLE, AEBlocks.EXPERIENCE_BOTTLE_CROP::get), new Item.Properties());

    public static final Supplier<Item> WHITE_DYE_SEED = ITEMS.registerItem("white_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.WHITE_DYE, AEBlocks.WHITE_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLACK_DYE_SEED = ITEMS.registerItem("black_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLACK_DYE, AEBlocks.BLACK_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> ORANGE_DYE_SEED = ITEMS.registerItem("orange_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.ORANGE_DYE, AEBlocks.ORANGE_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PINK_DYE_SEED = ITEMS.registerItem("pink_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.PINK_DYE, AEBlocks.PINK_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PURPLE_DYE_SEED = ITEMS.registerItem("purple_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.PURPLE_DYE, AEBlocks.PURPLE_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> MAGENTA_DYE_SEED = ITEMS.registerItem("magenta_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.MAGENTA_DYE, AEBlocks.MAGENTA_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> LIGHT_BLUE_DYE_SEED = ITEMS.registerItem("light_blue_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.LIGHT_BLUE_DYE, AEBlocks.LIGHT_BLUE_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> LIME_DYE_SEED = ITEMS.registerItem("lime_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.LIME_DYE, AEBlocks.LIME_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> HEART_OF_THE_SEA_SEED = ITEMS.registerItem("heart_of_the_sea_seed", props -> new SpeciesSeedItem(props, CropSpecies.HEART_OF_THE_SEA, AEBlocks.HEART_OF_THE_SEA_CROP::get), new Item.Properties());
    public static final Supplier<Item> NETHER_STAR_SEED = ITEMS.registerItem("nether_star_seed", props -> new SpeciesSeedItem(props, CropSpecies.NETHER_STAR, AEBlocks.NETHER_STAR_CROP::get), new Item.Properties());
    public static final Supplier<Item> DRAGON_HEAD_SEED = ITEMS.registerItem("dragon_head_seed", props -> new SpeciesSeedItem(props, CropSpecies.DRAGON_HEAD, AEBlocks.DRAGON_HEAD_CROP::get), new Item.Properties());
    public static final Supplier<Item> HEAVY_CORE_SEED = ITEMS.registerItem("heavy_core_seed", props -> new SpeciesSeedItem(props, CropSpecies.HEAVY_CORE, AEBlocks.HEAVY_CORE_CROP::get), new Item.Properties());

    public static final Supplier<Item> STICK_SEED = ITEMS.registerItem("stick_seed", props -> new SpeciesSeedItem(props, CropSpecies.STICK, AEBlocks.STICK_CROP::get), new Item.Properties());
    public static final Supplier<Item> DRIED_KELP_SEED = ITEMS.registerItem("dried_kelp_seed", props -> new SpeciesSeedItem(props, CropSpecies.DRIED_KELP, AEBlocks.DRIED_KELP_CROP::get), new Item.Properties());
    public static final Supplier<Item> GRAY_DYE_SEED = ITEMS.registerItem("gray_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.GRAY_DYE, AEBlocks.GRAY_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> LIGHT_GRAY_DYE_SEED = ITEMS.registerItem("light_gray_dye_seed", props -> new SpeciesSeedItem(props, CropSpecies.LIGHT_GRAY_DYE, AEBlocks.LIGHT_GRAY_DYE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SKELETON_SKULL_SEED = ITEMS.registerItem("skeleton_skull_seed", props -> new SpeciesSeedItem(props, CropSpecies.SKELETON_SKULL, AEBlocks.SKELETON_SKULL_CROP::get), new Item.Properties());
    public static final Supplier<Item> WITHER_SKELETON_SKULL_SEED = ITEMS.registerItem("wither_skeleton_skull_seed", props -> new SpeciesSeedItem(props, CropSpecies.WITHER_SKELETON_SKULL, AEBlocks.WITHER_SKELETON_SKULL_CROP::get), new Item.Properties());
    public static final Supplier<Item> CREEPER_HEAD_SEED = ITEMS.registerItem("creeper_head_seed", props -> new SpeciesSeedItem(props, CropSpecies.CREEPER_HEAD, AEBlocks.CREEPER_HEAD_CROP::get), new Item.Properties());
    public static final Supplier<Item> ZOMBIE_HEAD_SEED = ITEMS.registerItem("zombie_head_seed", props -> new SpeciesSeedItem(props, CropSpecies.ZOMBIE_HEAD, AEBlocks.ZOMBIE_HEAD_CROP::get), new Item.Properties());

    public static final Supplier<Item> PUMPKIN = ITEMS.registerItem("pumpkin", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PUMPKIN_SEED = ITEMS.registerItem("pumpkin_seed", props -> new SpeciesSeedItem(props, CropSpecies.PUMPKIN, AEBlocks.PUMPKIN_CROP::get), new Item.Properties());
    public static final Supplier<Item> BUTTERNUT_SQUASH = ITEMS.registerItem("butternut_squash", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BUTTERNUT_SQUASH_SEED = ITEMS.registerItem("butternut_squash_seed", props -> new SpeciesSeedItem(props, CropSpecies.BUTTERNUT_SQUASH, AEBlocks.BUTTERNUT_SQUASH_CROP::get), new Item.Properties());
    public static final Supplier<Item> COURGETTE = ITEMS.registerItem("courgette", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> COURGETTE_SEED = ITEMS.registerItem("courgette_seed", props -> new SpeciesSeedItem(props, CropSpecies.COURGETTE, AEBlocks.COURGETTE_CROP::get), new Item.Properties());
    public static final Supplier<Item> TURNIP = ITEMS.registerItem("turnip", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> TURNIP_SEED = ITEMS.registerItem("turnip_seed", props -> new SpeciesSeedItem(props, CropSpecies.TURNIP, AEBlocks.TURNIP_CROP::get), new Item.Properties());
    public static final Supplier<Item> PARSNIP = ITEMS.registerItem("parsnip", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PARSNIP_SEED = ITEMS.registerItem("parsnip_seed", props -> new SpeciesSeedItem(props, CropSpecies.PARSNIP, AEBlocks.PARSNIP_CROP::get), new Item.Properties());
    public static final Supplier<Item> LEEK = ITEMS.registerItem("leek", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> LEEK_SEED = ITEMS.registerItem("leek_seed", props -> new SpeciesSeedItem(props, CropSpecies.LEEK, AEBlocks.LEEK_CROP::get), new Item.Properties());
    public static final Supplier<Item> CELERY = ITEMS.registerItem("celery", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CELERY_SEED = ITEMS.registerItem("celery_seed", props -> new SpeciesSeedItem(props, CropSpecies.CELERY, AEBlocks.CELERY_CROP::get), new Item.Properties());
    public static final Supplier<Item> ASPARAGUS = ITEMS.registerItem("asparagus", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> ASPARAGUS_SEED = ITEMS.registerItem("asparagus_seed", props -> new SpeciesSeedItem(props, CropSpecies.ASPARAGUS, AEBlocks.ASPARAGUS_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLUEBERRY = ITEMS.registerItem("blueberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BLUEBERRY_SEED = ITEMS.registerItem("blueberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLUEBERRY, AEBlocks.BLUEBERRY_CROP::get), new Item.Properties());
    public static final Supplier<Item> RASPBERRY = ITEMS.registerItem("raspberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> RASPBERRY_SEED = ITEMS.registerItem("raspberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.RASPBERRY, AEBlocks.RASPBERRY_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLACKBERRY = ITEMS.registerItem("blackberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BLACKBERRY_SEED = ITEMS.registerItem("blackberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLACKBERRY, AEBlocks.BLACKBERRY_CROP::get), new Item.Properties());
    public static final Supplier<Item> CRANBERRY = ITEMS.registerItem("cranberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CRANBERRY_SEED = ITEMS.registerItem("cranberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.CRANBERRY, AEBlocks.CRANBERRY_CROP::get), new Item.Properties());
    static { CookingFoods.bootstrap(); }

    public static final Supplier<Item> SPINACH = ITEMS.registerItem("spinach", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> SPINACH_SEED = ITEMS.registerItem("spinach_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPINACH, AEBlocks.SPINACH_CROP::get), new Item.Properties());
    public static final Supplier<Item> CAULIFLOWER = ITEMS.registerItem("cauliflower", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CAULIFLOWER_SEED = ITEMS.registerItem("cauliflower_seed", props -> new SpeciesSeedItem(props, CropSpecies.CAULIFLOWER, AEBlocks.CAULIFLOWER_CROP::get), new Item.Properties());
    public static final Supplier<Item> ARTICHOKE = ITEMS.registerItem("artichoke", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> ARTICHOKE_SEED = ITEMS.registerItem("artichoke_seed", props -> new SpeciesSeedItem(props, CropSpecies.ARTICHOKE, AEBlocks.ARTICHOKE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CHILLI = ITEMS.registerItem("chilli", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CHILLI_SEED = ITEMS.registerItem("chilli_seed", props -> new SpeciesSeedItem(props, CropSpecies.CHILLI, AEBlocks.CHILLI_CROP::get), new Item.Properties());
    public static final Supplier<Item> SPRING_ONION = ITEMS.registerItem("spring_onion", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).build()));
    public static final Supplier<Item> SPRING_ONION_SEED = ITEMS.registerItem("spring_onion_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPRING_ONION, AEBlocks.SPRING_ONION_CROP::get), new Item.Properties());
    public static final Supplier<Item> FENNEL = ITEMS.registerItem("fennel", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> FENNEL_SEED = ITEMS.registerItem("fennel_seed", props -> new SpeciesSeedItem(props, CropSpecies.FENNEL, AEBlocks.FENNEL_CROP::get), new Item.Properties());
    public static final Supplier<Item> DILL = ITEMS.registerItem("dill", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> DILL_SEED = ITEMS.registerItem("dill_seed", props -> new SpeciesSeedItem(props, CropSpecies.DILL, AEBlocks.DILL_CROP::get), new Item.Properties());
    public static final Supplier<Item> PEANUT = ITEMS.registerItem("peanut", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PEANUT_SEED = ITEMS.registerItem("peanut_seed", props -> new SpeciesSeedItem(props, CropSpecies.PEANUT, AEBlocks.PEANUT_CROP::get), new Item.Properties());
    public static final Supplier<Item> SESAME = ITEMS.registerItem("sesame", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> SESAME_SEED = ITEMS.registerItem("sesame_seed", props -> new SpeciesSeedItem(props, CropSpecies.SESAME, AEBlocks.SESAME_CROP::get), new Item.Properties());
    public static final Supplier<Item> OATS = ITEMS.registerItem("oats", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> OATS_SEED = ITEMS.registerItem("oats_seed", props -> new SpeciesSeedItem(props, CropSpecies.OATS, AEBlocks.OATS_CROP::get), new Item.Properties());
    public static final Supplier<Item> GOOSEBERRY = ITEMS.registerItem("gooseberry", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> GOOSEBERRY_SEED = ITEMS.registerItem("gooseberry_seed", props -> new SpeciesSeedItem(props, CropSpecies.GOOSEBERRY, AEBlocks.GOOSEBERRY_CROP::get), new Item.Properties());
    public static final Supplier<Item> BLACKCURRANT = ITEMS.registerItem("blackcurrant", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BLACKCURRANT_SEED = ITEMS.registerItem("blackcurrant_seed", props -> new SpeciesSeedItem(props, CropSpecies.BLACKCURRANT, AEBlocks.BLACKCURRANT_CROP::get), new Item.Properties());
    public static final Supplier<Item> KALE = ITEMS.registerItem("kale", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> KALE_SEED = ITEMS.registerItem("kale_seed", props -> new SpeciesSeedItem(props, CropSpecies.KALE, AEBlocks.KALE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PAK_CHOI = ITEMS.registerItem("pak_choi", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> PAK_CHOI_SEED = ITEMS.registerItem("pak_choi_seed", props -> new SpeciesSeedItem(props, CropSpecies.PAK_CHOI, AEBlocks.PAK_CHOI_CROP::get), new Item.Properties());
    public static final Supplier<Item> BRUSSELS_SPROUTS = ITEMS.registerItem("brussels_sprouts", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BRUSSELS_SPROUTS_SEED = ITEMS.registerItem("brussels_sprouts_seed", props -> new SpeciesSeedItem(props, CropSpecies.BRUSSELS_SPROUTS, AEBlocks.BRUSSELS_SPROUTS_CROP::get), new Item.Properties());
    public static final Supplier<Item> SWEDE = ITEMS.registerItem("swede", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> SWEDE_SEED = ITEMS.registerItem("swede_seed", props -> new SpeciesSeedItem(props, CropSpecies.SWEDE, AEBlocks.SWEDE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CELERIAC = ITEMS.registerItem("celeriac", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CELERIAC_SEED = ITEMS.registerItem("celeriac_seed", props -> new SpeciesSeedItem(props, CropSpecies.CELERIAC, AEBlocks.CELERIAC_CROP::get), new Item.Properties());
    public static final Supplier<Item> LENTIL = ITEMS.registerItem("lentil", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> LENTIL_SEED = ITEMS.registerItem("lentil_seed", props -> new SpeciesSeedItem(props, CropSpecies.LENTIL, AEBlocks.LENTIL_CROP::get), new Item.Properties());
    public static final Supplier<Item> CHICKPEA = ITEMS.registerItem("chickpea", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CHICKPEA_SEED = ITEMS.registerItem("chickpea_seed", props -> new SpeciesSeedItem(props, CropSpecies.CHICKPEA, AEBlocks.CHICKPEA_CROP::get), new Item.Properties());
    public static final Supplier<Item> BARLEY = ITEMS.registerItem("barley", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BARLEY_SEED = ITEMS.registerItem("barley_seed", props -> new SpeciesSeedItem(props, CropSpecies.BARLEY, AEBlocks.BARLEY_CROP::get), new Item.Properties());
    public static final Supplier<Item> BUCKWHEAT = ITEMS.registerItem("buckwheat", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> BUCKWHEAT_SEED = ITEMS.registerItem("buckwheat_seed", props -> new SpeciesSeedItem(props, CropSpecies.BUCKWHEAT, AEBlocks.BUCKWHEAT_CROP::get), new Item.Properties());
    public static final Supplier<Item> RHUBARB = ITEMS.registerItem("rhubarb", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> RHUBARB_SEED = ITEMS.registerItem("rhubarb_seed", props -> new SpeciesSeedItem(props, CropSpecies.RHUBARB, AEBlocks.RHUBARB_CROP::get), new Item.Properties());
    public static final Supplier<Item> GRAPE = ITEMS.registerItem("grape", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> GRAPE_SEED = ITEMS.registerItem("grape_seed", props -> new SpeciesSeedItem(props, CropSpecies.GRAPE, AEBlocks.GRAPE_CROP::get), new Item.Properties());
    public static final Supplier<Item> CANTALOUPE = ITEMS.registerItem("cantaloupe", Item::new, new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    public static final Supplier<Item> CANTALOUPE_SEED = ITEMS.registerItem("cantaloupe_seed", props -> new SpeciesSeedItem(props, CropSpecies.CANTALOUPE, AEBlocks.CANTALOUPE_CROP::get), new Item.Properties());
    public static final Supplier<Item> JUNGLE_LOG_SEED = ITEMS.registerItem("jungle_log_seed", props -> new SpeciesSeedItem(props, CropSpecies.JUNGLE_LOG, AEBlocks.JUNGLE_LOG_CROP::get), new Item.Properties());
    public static final Supplier<Item> COARSE_DIRT_SEED = ITEMS.registerItem("coarse_dirt_seed", props -> new SpeciesSeedItem(props, CropSpecies.COARSE_DIRT, AEBlocks.COARSE_DIRT_CROP::get), new Item.Properties());
    public static final Supplier<Item> ROOTED_DIRT_SEED = ITEMS.registerItem("rooted_dirt_seed", props -> new SpeciesSeedItem(props, CropSpecies.ROOTED_DIRT, AEBlocks.ROOTED_DIRT_CROP::get), new Item.Properties());
    public static final Supplier<Item> RED_SANDSTONE_SEED = ITEMS.registerItem("red_sandstone_seed", props -> new SpeciesSeedItem(props, CropSpecies.RED_SANDSTONE, AEBlocks.RED_SANDSTONE_CROP::get), new Item.Properties());
    public static final Supplier<Item> PRISMARINE_BRICKS_SEED = ITEMS.registerItem("prismarine_bricks_seed", props -> new SpeciesSeedItem(props, CropSpecies.PRISMARINE_BRICKS, AEBlocks.PRISMARINE_BRICKS_CROP::get), new Item.Properties());
    public static final Supplier<Item> WARPED_WART_BLOCK_SEED = ITEMS.registerItem("warped_wart_block_seed", props -> new SpeciesSeedItem(props, CropSpecies.WARPED_WART_BLOCK, AEBlocks.WARPED_WART_BLOCK_CROP::get), new Item.Properties());
    public static final Supplier<Item> RED_NETHER_BRICKS_SEED = ITEMS.registerItem("red_nether_bricks_seed", props -> new SpeciesSeedItem(props, CropSpecies.RED_NETHER_BRICKS, AEBlocks.RED_NETHER_BRICKS_CROP::get), new Item.Properties());
    public static final Supplier<Item> SCULK_SENSOR_SEED = ITEMS.registerItem("sculk_sensor_seed", props -> new SpeciesSeedItem(props, CropSpecies.SCULK_SENSOR, AEBlocks.SCULK_SENSOR_CROP::get), new Item.Properties());
    public static final Supplier<Item> SCULK_SHRIEKER_SEED = ITEMS.registerItem("sculk_shrieker_seed", props -> new SpeciesSeedItem(props, CropSpecies.SCULK_SHRIEKER, AEBlocks.SCULK_SHRIEKER_CROP::get), new Item.Properties());
    public static final Supplier<Item> CONDUIT_SEED = ITEMS.registerItem("conduit_seed", props -> new SpeciesSeedItem(props, CropSpecies.CONDUIT, AEBlocks.CONDUIT_CROP::get), new Item.Properties());
    public static final Supplier<Item> RESPAWN_ANCHOR_SEED = ITEMS.registerItem("respawn_anchor_seed", props -> new SpeciesSeedItem(props, CropSpecies.RESPAWN_ANCHOR, AEBlocks.RESPAWN_ANCHOR_CROP::get), new Item.Properties());

    public static final Supplier<Item> GRASS_BLOCK_SEED = ITEMS.registerItem("grass_block_seed", props -> new SpeciesSeedItem(props, CropSpecies.GRASS_BLOCK, AEBlocks.GRASS_BLOCK_CROP::get), new Item.Properties());
    public static final Supplier<Item> PODZOL_SEED = ITEMS.registerItem("podzol_seed", props -> new SpeciesSeedItem(props, CropSpecies.PODZOL, AEBlocks.PODZOL_CROP::get), new Item.Properties());
    public static final Supplier<Item> MYCELIUM_SEED = ITEMS.registerItem("mycelium_seed", props -> new SpeciesSeedItem(props, CropSpecies.MYCELIUM, AEBlocks.MYCELIUM_CROP::get), new Item.Properties());
    public static final Supplier<Item> DEAD_BUSH_SEED = ITEMS.registerItem("dead_bush_seed", props -> new SpeciesSeedItem(props, CropSpecies.DEAD_BUSH, AEBlocks.DEAD_BUSH_CROP::get), new Item.Properties());
    public static final Supplier<Item> MUD_BRICKS_SEED = ITEMS.registerItem("mud_bricks_seed", props -> new SpeciesSeedItem(props, CropSpecies.MUD_BRICKS, AEBlocks.MUD_BRICKS_CROP::get), new Item.Properties());
    public static final Supplier<Item> DEEPSLATE_BRICKS_SEED = ITEMS.registerItem("deepslate_bricks_seed", props -> new SpeciesSeedItem(props, CropSpecies.DEEPSLATE_BRICKS, AEBlocks.DEEPSLATE_BRICKS_CROP::get), new Item.Properties());
    public static final Supplier<Item> CALIBRATED_SCULK_SENSOR_SEED = ITEMS.registerItem("calibrated_sculk_sensor_seed", props -> new SpeciesSeedItem(props, CropSpecies.CALIBRATED_SCULK_SENSOR, AEBlocks.CALIBRATED_SCULK_SENSOR_CROP::get), new Item.Properties());
    public static final Supplier<Item> END_CRYSTAL_SEED = ITEMS.registerItem("end_crystal_seed", props -> new SpeciesSeedItem(props, CropSpecies.END_CRYSTAL, AEBlocks.END_CRYSTAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> TOTEM_OF_UNDYING_SEED = ITEMS.registerItem("totem_of_undying_seed", props -> new SpeciesSeedItem(props, CropSpecies.TOTEM_OF_UNDYING, AEBlocks.TOTEM_OF_UNDYING_CROP::get), new Item.Properties());
    public static final Supplier<Item> ENCHANTED_GOLDEN_APPLE_SEED = ITEMS.registerItem("enchanted_golden_apple_seed", props -> new SpeciesSeedItem(props, CropSpecies.ENCHANTED_GOLDEN_APPLE, AEBlocks.ENCHANTED_GOLDEN_APPLE_CROP::get), new Item.Properties());

    public static final Supplier<Item> SUGAR_CANE_SEED = ITEMS.registerItem("sugar_cane_seed", props -> new SpeciesSeedItem(props, CropSpecies.SUGAR_CANE, AEBlocks.SUGAR_CANE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SWEET_BERRIES_SEED = ITEMS.registerItem("sweet_berries_seed", props -> new SpeciesSeedItem(props, CropSpecies.SWEET_BERRIES, AEBlocks.SWEET_BERRIES_CROP::get), new Item.Properties());
    public static final Supplier<Item> SEAGRASS_SEED = ITEMS.registerItem("seagrass_seed", props -> new SpeciesSeedItem(props, CropSpecies.SEAGRASS, AEBlocks.SEAGRASS_CROP::get), new Item.Properties());
    public static final Supplier<Item> HANGING_ROOTS_SEED = ITEMS.registerItem("hanging_roots_seed", props -> new SpeciesSeedItem(props, CropSpecies.HANGING_ROOTS, AEBlocks.HANGING_ROOTS_CROP::get), new Item.Properties());
    public static final Supplier<Item> TORCHFLOWER_SEED = ITEMS.registerItem("torchflower_seed", props -> new SpeciesSeedItem(props, CropSpecies.TORCHFLOWER, AEBlocks.TORCHFLOWER_CROP::get), new Item.Properties());
    public static final Supplier<Item> PITCHER_PLANT_SEED = ITEMS.registerItem("pitcher_plant_seed", props -> new SpeciesSeedItem(props, CropSpecies.PITCHER_PLANT, AEBlocks.PITCHER_PLANT_CROP::get), new Item.Properties());
    public static final Supplier<Item> SPORE_BLOSSOM_SEED = ITEMS.registerItem("spore_blossom_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPORE_BLOSSOM, AEBlocks.SPORE_BLOSSOM_CROP::get), new Item.Properties());
    public static final Supplier<Item> BIG_DRIPLEAF_SEED = ITEMS.registerItem("big_dripleaf_seed", props -> new SpeciesSeedItem(props, CropSpecies.BIG_DRIPLEAF, AEBlocks.BIG_DRIPLEAF_CROP::get), new Item.Properties());
    public static final Supplier<Item> TRIAL_KEY_SEED = ITEMS.registerItem("trial_key_seed", props -> new SpeciesSeedItem(props, CropSpecies.TRIAL_KEY, AEBlocks.TRIAL_KEY_CROP::get), new Item.Properties());
    public static final Supplier<Item> OMINOUS_TRIAL_KEY_SEED = ITEMS.registerItem("ominous_trial_key_seed", props -> new SpeciesSeedItem(props, CropSpecies.OMINOUS_TRIAL_KEY, AEBlocks.OMINOUS_TRIAL_KEY_CROP::get), new Item.Properties());
    public static final Supplier<Item> NETHERITE_UPGRADE_SMITHING_TEMPLATE_SEED = ITEMS.registerItem("netherite_upgrade_smithing_template_seed", props -> new SpeciesSeedItem(props, CropSpecies.NETHERITE_UPGRADE_SMITHING_TEMPLATE, AEBlocks.NETHERITE_UPGRADE_SMITHING_TEMPLATE_CROP::get), new Item.Properties());
    public static final Supplier<Item> DISC_FRAGMENT_5_SEED = ITEMS.registerItem("disc_fragment_5_seed", props -> new SpeciesSeedItem(props, CropSpecies.DISC_FRAGMENT_5, AEBlocks.DISC_FRAGMENT_5_CROP::get), new Item.Properties());

    public static final Supplier<Item> OAK_SAPLING_SEED = ITEMS.registerItem("oak_sapling_seed", props -> new SpeciesSeedItem(props, CropSpecies.OAK_SAPLING, AEBlocks.OAK_SAPLING_CROP::get), new Item.Properties());
    public static final Supplier<Item> SPRUCE_SAPLING_SEED = ITEMS.registerItem("spruce_sapling_seed", props -> new SpeciesSeedItem(props, CropSpecies.SPRUCE_SAPLING, AEBlocks.SPRUCE_SAPLING_CROP::get), new Item.Properties());
    public static final Supplier<Item> BIRCH_SAPLING_SEED = ITEMS.registerItem("birch_sapling_seed", props -> new SpeciesSeedItem(props, CropSpecies.BIRCH_SAPLING, AEBlocks.BIRCH_SAPLING_CROP::get), new Item.Properties());
    public static final Supplier<Item> DARK_OAK_SAPLING_SEED = ITEMS.registerItem("dark_oak_sapling_seed", props -> new SpeciesSeedItem(props, CropSpecies.DARK_OAK_SAPLING, AEBlocks.DARK_OAK_SAPLING_CROP::get), new Item.Properties());
    public static final Supplier<Item> TUBE_CORAL_SEED = ITEMS.registerItem("tube_coral_seed", props -> new SpeciesSeedItem(props, CropSpecies.TUBE_CORAL, AEBlocks.TUBE_CORAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> BRAIN_CORAL_SEED = ITEMS.registerItem("brain_coral_seed", props -> new SpeciesSeedItem(props, CropSpecies.BRAIN_CORAL, AEBlocks.BRAIN_CORAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> BUBBLE_CORAL_SEED = ITEMS.registerItem("bubble_coral_seed", props -> new SpeciesSeedItem(props, CropSpecies.BUBBLE_CORAL, AEBlocks.BUBBLE_CORAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> FIRE_CORAL_SEED = ITEMS.registerItem("fire_coral_seed", props -> new SpeciesSeedItem(props, CropSpecies.FIRE_CORAL, AEBlocks.FIRE_CORAL_CROP::get), new Item.Properties());
    public static final Supplier<Item> COAST_ARMOR_TRIM_SMITHING_TEMPLATE_SEED = ITEMS.registerItem("coast_armor_trim_smithing_template_seed", props -> new SpeciesSeedItem(props, CropSpecies.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, AEBlocks.COAST_ARMOR_TRIM_SMITHING_TEMPLATE_CROP::get), new Item.Properties());
    public static final Supplier<Item> TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED = ITEMS.registerItem("tide_armor_trim_smithing_template_seed", props -> new SpeciesSeedItem(props, CropSpecies.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, AEBlocks.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_SEED = ITEMS.registerItem("snout_armor_trim_smithing_template_seed", props -> new SpeciesSeedItem(props, CropSpecies.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, AEBlocks.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_CROP::get), new Item.Properties());
    public static final Supplier<Item> SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED = ITEMS.registerItem("silence_armor_trim_smithing_template_seed", props -> new SpeciesSeedItem(props, CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, AEBlocks.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_CROP::get), new Item.Properties());

    public static final Supplier<Item> BREEDING_JOURNAL =
            ITEMS.registerItem("breeding_journal", BreedingJournalItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<Item> SOIL_SAMPLE =
            ITEMS.registerItem("soil_sample", SoilSampleItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<SoilSpecimenItem> SOIL_SPECIMEN =
            ITEMS.registerItem("soil_specimen", SoilSpecimenItem::new, new Item.Properties().stacksTo(64));
    public static final Supplier<BlockItem> SOIL_RESEARCH_TABLE =
            ITEMS.registerItem("soil_research_table", props -> new BlockItem(AEBlocks.SOIL_RESEARCH_TABLE.get(), props),
                    new Item.Properties());
    public static final Supplier<Item> NUTRIENT_COMPOST =
            ITEMS.registerItem("nutrient_compost", NutrientCompostItem::new, new Item.Properties());

    public static final Supplier<Item> SOIL_RESEARCH_REFINEMENT_UPGRADE =
            ITEMS.registerItem("soil_research_refinement_upgrade", Item::new, new Item.Properties().stacksTo(16));
    public static final Supplier<Item> SOIL_RESEARCH_MASTERY_UPGRADE =
            ITEMS.registerItem("soil_research_mastery_upgrade", Item::new, new Item.Properties().stacksTo(16));

    public static final Supplier<Item> GREENHOUSE_AUTOMATION_UPGRADE =
            ITEMS.registerItem("greenhouse_automation_upgrade", Item::new, new Item.Properties().stacksTo(16));

    public static final Supplier<Item> GREENHOUSE_SMART_FARMING_UPGRADE =
            ITEMS.registerItem("greenhouse_smart_farming_upgrade", Item::new, new Item.Properties().stacksTo(16));

    public static final Supplier<Item> GENETIC_CLIPPERS =
            ITEMS.registerItem("genetic_clippers", GeneticClippersItem::new,
                    new Item.Properties().stacksTo(1));

    public static final Supplier<Item> CROP_SUPPORT =
            ITEMS.registerItem("crop_support", CropSupportItem::new, new Item.Properties());
    public static final Supplier<Item> IRON_CROP_SUPPORT = ITEMS.registerItem("iron_crop_support", p -> new CropSupportItem(p,2), new Item.Properties());
    public static final Supplier<Item> OBSIDIAN_CROP_SUPPORT = ITEMS.registerItem("obsidian_crop_support", p -> new CropSupportItem(p,3), new Item.Properties());
    public static final Supplier<IrrigationBasinItem> IRRIGATION_BASIN =
            ITEMS.registerItem("irrigation_basin", props -> new IrrigationBasinItem(AEBlocks.IRRIGATION_BASIN.get(), props),
                    new Item.Properties());

    public static final Supplier<BlockItem> SEED_ANALYZER =
            ITEMS.registerItem("seed_analyzer", props -> new BlockItem(AEBlocks.SEED_ANALYZER.get(), props),
                    new Item.Properties());

    public static final Supplier<BlockItem> SEED_BREEDING_STATION =
            ITEMS.registerItem("seed_breeding_station", props -> new BlockItem(AEBlocks.SEED_BREEDING_STATION.get(), props),
                    new Item.Properties());

    public static final Supplier<BlockItem> CULTIVATED_FARMLAND =
            ITEMS.registerItem("cultivated_farmland", props -> new BlockItem(AEBlocks.CULTIVATED_FARMLAND.get(), props),
                    new Item.Properties());
    public static final Supplier<BlockItem> NUTRIENT_FARMLAND =
            ITEMS.registerItem("nutrient_farmland", props -> new BlockItem(AEBlocks.NUTRIENT_FARMLAND.get(), props),
                    new Item.Properties());
    public static final Supplier<BlockItem> MYTHIC_FARMLAND =
            ITEMS.registerItem("mythic_farmland", props -> new BlockItem(AEBlocks.MYTHIC_FARMLAND.get(), props),
                    new Item.Properties());

    public static Item seedFor(CropSpecies species) {
        return switch (species) {
            case WILD -> WILD_SEED.get();
            case CRIMSON_CLOVER -> CRIMSON_CLOVER_SEED.get();
            case AZURE_FLAX -> AZURE_FLAX_SEED.get();
            case VIOLET_STARFLOWER -> VIOLET_STARFLOWER_SEED.get();
            case AMBER_MARIGOLD -> AMBER_MARIGOLD_SEED.get();
            case SILVER_THISTLE -> SILVER_THISTLE_SEED.get();
            case SUNFIRE_LILY -> SUNFIRE_LILY_SEED.get();
            case MOON_ORCHID -> MOON_ORCHID_SEED.get();
            case ECLIPSE_LOTUS -> ECLIPSE_LOTUS_SEED.get();
            case TWILIGHT_SAGE -> TWILIGHT_SAGE_SEED.get();
            case DIRT -> DIRT_SEED.get();
            case WOOD -> WOOD_SEED.get();
            case STONE -> STONE_SEED.get();
            case COAL -> COAL_SEED.get();
            case COPPER -> COPPER_SEED.get();
            case IRON -> IRON_SEED.get();
            case REDSTONE -> REDSTONE_SEED.get();
            case GOLD -> GOLD_SEED.get();
            case LAPIS -> LAPIS_SEED.get();
            case DIAMOND -> DIAMOND_SEED.get();
            case EMERALD -> EMERALD_SEED.get();
            case QUARTZ -> QUARTZ_SEED.get();
            case GRAVEL -> GRAVEL_SEED.get();
            case SAND -> SAND_SEED.get();
            case CLAY -> CLAY_SEED.get();
            case FLINT -> FLINT_SEED.get();
            case GLOWSTONE -> GLOWSTONE_SEED.get();
            case OBSIDIAN -> OBSIDIAN_SEED.get();
            case NETHERRACK -> NETHERRACK_SEED.get();
            case SOUL_SAND -> SOUL_SAND_SEED.get();
            case MAGMA_CREAM -> MAGMA_CREAM_SEED.get();
            case NETHER_WART -> NETHER_WART_SEED.get();
            case BLAZE_ROD -> BLAZE_ROD_SEED.get();
            case END_STONE -> END_STONE_SEED.get();
            case CHORUS_FRUIT -> CHORUS_FRUIT_SEED.get();
            case ENDER_PEARL -> ENDER_PEARL_SEED.get();
            case SHULKER_SHELL -> SHULKER_SHELL_SEED.get();
            case ANCIENT_DEBRIS -> ANCIENT_DEBRIS_SEED.get();
            case NETHERITE -> NETHERITE_SEED.get();
            case OAK_SAPLING -> OAK_SAPLING_SEED.get();
            case SPRUCE_SAPLING -> SPRUCE_SAPLING_SEED.get();
            case BIRCH_SAPLING -> BIRCH_SAPLING_SEED.get();
            case DARK_OAK_SAPLING -> DARK_OAK_SAPLING_SEED.get();
            case TUBE_CORAL -> TUBE_CORAL_SEED.get();
            case BRAIN_CORAL -> BRAIN_CORAL_SEED.get();
            case BUBBLE_CORAL -> BUBBLE_CORAL_SEED.get();
            case FIRE_CORAL -> FIRE_CORAL_SEED.get();
            case COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> COAST_ARMOR_TRIM_SMITHING_TEMPLATE_SEED.get();
            case TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> TIDE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED.get();
            case SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE_SEED.get();
            case SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE_SEED.get();
            case SUGAR_CANE -> SUGAR_CANE_SEED.get();
            case SWEET_BERRIES -> SWEET_BERRIES_SEED.get();
            case SEAGRASS -> SEAGRASS_SEED.get();
            case HANGING_ROOTS -> HANGING_ROOTS_SEED.get();
            case TORCHFLOWER -> TORCHFLOWER_SEED.get();
            case PITCHER_PLANT -> PITCHER_PLANT_SEED.get();
            case SPORE_BLOSSOM -> SPORE_BLOSSOM_SEED.get();
            case BIG_DRIPLEAF -> BIG_DRIPLEAF_SEED.get();
            case TRIAL_KEY -> TRIAL_KEY_SEED.get();
            case OMINOUS_TRIAL_KEY -> OMINOUS_TRIAL_KEY_SEED.get();
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE -> NETHERITE_UPGRADE_SMITHING_TEMPLATE_SEED.get();
            case DISC_FRAGMENT_5 -> DISC_FRAGMENT_5_SEED.get();
            case GRASS_BLOCK -> GRASS_BLOCK_SEED.get();
            case PODZOL -> PODZOL_SEED.get();
            case MYCELIUM -> MYCELIUM_SEED.get();
            case DEAD_BUSH -> DEAD_BUSH_SEED.get();


            case MUD_BRICKS -> MUD_BRICKS_SEED.get();
            case DEEPSLATE_BRICKS -> DEEPSLATE_BRICKS_SEED.get();
            case CALIBRATED_SCULK_SENSOR -> CALIBRATED_SCULK_SENSOR_SEED.get();
            case END_CRYSTAL -> END_CRYSTAL_SEED.get();
            case TOTEM_OF_UNDYING -> TOTEM_OF_UNDYING_SEED.get();
            case ENCHANTED_GOLDEN_APPLE -> ENCHANTED_GOLDEN_APPLE_SEED.get();
            case JUNGLE_LOG -> JUNGLE_LOG_SEED.get();
            case COARSE_DIRT -> COARSE_DIRT_SEED.get();
            case ROOTED_DIRT -> ROOTED_DIRT_SEED.get();
            case RED_SANDSTONE -> RED_SANDSTONE_SEED.get();
            case PRISMARINE_BRICKS -> PRISMARINE_BRICKS_SEED.get();

            case WARPED_WART_BLOCK -> WARPED_WART_BLOCK_SEED.get();
            case RED_NETHER_BRICKS -> RED_NETHER_BRICKS_SEED.get();
            case SCULK_SENSOR -> SCULK_SENSOR_SEED.get();
            case SCULK_SHRIEKER -> SCULK_SHRIEKER_SEED.get();
            case CONDUIT -> CONDUIT_SEED.get();
            case RESPAWN_ANCHOR -> RESPAWN_ANCHOR_SEED.get();
            case STICK -> STICK_SEED.get();
            case DRIED_KELP -> DRIED_KELP_SEED.get();
            case GRAY_DYE -> GRAY_DYE_SEED.get();
            case LIGHT_GRAY_DYE -> LIGHT_GRAY_DYE_SEED.get();




            case SKELETON_SKULL -> SKELETON_SKULL_SEED.get();
            case WITHER_SKELETON_SKULL -> WITHER_SKELETON_SKULL_SEED.get();
            case CREEPER_HEAD -> CREEPER_HEAD_SEED.get();
            case ZOMBIE_HEAD -> ZOMBIE_HEAD_SEED.get();
            case WHITE_DYE -> WHITE_DYE_SEED.get();
            case BLACK_DYE -> BLACK_DYE_SEED.get();
            case ORANGE_DYE -> ORANGE_DYE_SEED.get();
            case PINK_DYE -> PINK_DYE_SEED.get();
            case PURPLE_DYE -> PURPLE_DYE_SEED.get();
            case MAGENTA_DYE -> MAGENTA_DYE_SEED.get();
            case LIGHT_BLUE_DYE -> LIGHT_BLUE_DYE_SEED.get();
            case LIME_DYE -> LIME_DYE_SEED.get();
            case HEART_OF_THE_SEA -> HEART_OF_THE_SEA_SEED.get();
            case NETHER_STAR -> NETHER_STAR_SEED.get();
            case DRAGON_HEAD -> DRAGON_HEAD_SEED.get();
            case HEAVY_CORE -> HEAVY_CORE_SEED.get();
            case GREEN_DYE -> GREEN_DYE_SEED.get();
            case RED_DYE -> RED_DYE_SEED.get();
            case YELLOW_DYE -> YELLOW_DYE_SEED.get();
            case BROWN_DYE -> BROWN_DYE_SEED.get();
            case HONEY_BOTTLE -> HONEY_BOTTLE_SEED.get();
            case GLOW_BERRIES -> GLOW_BERRIES_SEED.get();
            case BLUE_DYE -> BLUE_DYE_SEED.get();
            case CYAN_DYE -> CYAN_DYE_SEED.get();
            case FIRE_CHARGE -> FIRE_CHARGE_SEED.get();
            case WIND_CHARGE -> WIND_CHARGE_SEED.get();
            case DRAGON_BREATH -> DRAGON_BREATH_SEED.get();
            case EXPERIENCE_BOTTLE -> EXPERIENCE_BOTTLE_SEED.get();
            case SUGAR -> SUGAR_SEED.get();
            case COCOA_BEANS -> COCOA_BEANS_SEED.get();
            case EGG -> EGG_SEED.get();
            case ROTTEN_FLESH -> ROTTEN_FLESH_SEED.get();
            case RABBIT_HIDE -> RABBIT_HIDE_SEED.get();
            case RABBIT_FOOT -> RABBIT_FOOT_SEED.get();
            case ARMADILLO_SCUTE -> ARMADILLO_SCUTE_SEED.get();
            case FERMENTED_SPIDER_EYE -> FERMENTED_SPIDER_EYE_SEED.get();
            case WITHER_ROSE -> WITHER_ROSE_SEED.get();
            case CHORUS_FLOWER -> CHORUS_FLOWER_SEED.get();
            case CRIMSON_FUNGUS -> CRIMSON_FUNGUS_SEED.get();
            case WARPED_FUNGUS -> WARPED_FUNGUS_SEED.get();
            case SPRUCE_LOG -> SPRUCE_LOG_SEED.get();
            case BIRCH_LOG -> BIRCH_LOG_SEED.get();
            case ACACIA_LOG -> ACACIA_LOG_SEED.get();
            case DARK_OAK_LOG -> DARK_OAK_LOG_SEED.get();
            case MANGROVE_LOG -> MANGROVE_LOG_SEED.get();
            case CHERRY_LOG -> CHERRY_LOG_SEED.get();
            case SEA_PICKLE -> SEA_PICKLE_SEED.get();
            case GLOW_LICHEN -> GLOW_LICHEN_SEED.get();
            case CRIMSON_STEM -> CRIMSON_STEM_SEED.get();
            case WARPED_STEM -> WARPED_STEM_SEED.get();
            case VERDANT_FROGLIGHT -> VERDANT_FROGLIGHT_SEED.get();
            case PEARLESCENT_FROGLIGHT -> PEARLESCENT_FROGLIGHT_SEED.get();
            case CHARCOAL -> CHARCOAL_SEED.get();
            case GLASS -> GLASS_SEED.get();
            case BROWN_MUSHROOM -> BROWN_MUSHROOM_SEED.get();
            case RED_MUSHROOM -> RED_MUSHROOM_SEED.get();


            case MOSSY_COBBLESTONE -> MOSSY_COBBLESTONE_SEED.get();
            case LILY_PAD -> LILY_PAD_SEED.get();
            case DARK_PRISMARINE -> DARK_PRISMARINE_SEED.get();
            case GILDED_BLACKSTONE -> GILDED_BLACKSTONE_SEED.get();
            case SCULK_CATALYST -> SCULK_CATALYST_SEED.get();
            case OCHRE_FROGLIGHT -> OCHRE_FROGLIGHT_SEED.get();
            case RED_SAND -> RED_SAND_SEED.get();
            case SANDSTONE -> SANDSTONE_SEED.get();
            case CACTUS -> CACTUS_SEED.get();
            case VINE -> VINE_SEED.get();
            case PACKED_MUD -> PACKED_MUD_SEED.get();
            case BRICK -> BRICK_SEED.get();
            case ICE -> ICE_SEED.get();
            case PACKED_ICE -> PACKED_ICE_SEED.get();
            case BLUE_ICE -> BLUE_ICE_SEED.get();

            case NETHER_BRICK -> NETHER_BRICK_SEED.get();
            case ENDER_EYE -> ENDER_EYE_SEED.get();
            case GRANITE -> GRANITE_SEED.get();
            case DIORITE -> DIORITE_SEED.get();
            case ANDESITE -> ANDESITE_SEED.get();
            case MUD -> MUD_SEED.get();
            case TUFF -> TUFF_SEED.get();
            case DRIPSTONE -> DRIPSTONE_SEED.get();
            case DEEPSLATE -> DEEPSLATE_SEED.get();
            case SOUL_SOIL -> SOUL_SOIL_SEED.get();
            case SEA_LANTERN -> SEA_LANTERN_SEED.get();
            case SHROOMLIGHT -> SHROOMLIGHT_SEED.get();
            case END_ROD -> END_ROD_SEED.get();
            case SCULK -> SCULK_SEED.get();
            case PAPER -> PAPER_SEED.get();
            case KELP -> KELP_SEED.get();
            case BAMBOO -> BAMBOO_SEED.get();
            case BLACKSTONE -> BLACKSTONE_SEED.get();
            case BASALT -> BASALT_SEED.get();
            case PURPUR -> PURPUR_SEED.get();
            case TURTLE_SCUTE -> TURTLE_SCUTE_SEED.get();
            case SPONGE -> SPONGE_SEED.get();
            case SNOWBALL -> SNOWBALL_SEED.get();
            case MOSS -> MOSS_SEED.get();
            case CALCITE -> CALCITE_SEED.get();
            case TERRACOTTA -> TERRACOTTA_SEED.get();
            case SPIDER_EYE -> SPIDER_EYE_SEED.get();
            case GLOW_INK_SAC -> GLOW_INK_SAC_SEED.get();
            case CRYING_OBSIDIAN -> CRYING_OBSIDIAN_SEED.get();
            case NAUTILUS_SHELL -> NAUTILUS_SHELL_SEED.get();
            case FEATHER -> FEATHER_SEED.get();
            case INK_SAC -> INK_SAC_SEED.get();
            case HONEYCOMB -> HONEYCOMB_SEED.get();
            case SLIME_BALL -> SLIME_BALL_SEED.get();
            case GUNPOWDER -> GUNPOWDER_SEED.get();
            case PRISMARINE_CRYSTALS -> PRISMARINE_CRYSTALS_SEED.get();
            case PHANTOM_MEMBRANE -> PHANTOM_MEMBRANE_SEED.get();
            case ECHO_SHARD -> ECHO_SHARD_SEED.get();
            case STRING -> STRING_SEED.get();
            case WOOL -> WOOL_SEED.get();
            case LEATHER -> LEATHER_SEED.get();
            case BONE -> BONE_SEED.get();
            case AMETHYST -> AMETHYST_SEED.get();
            case PRISMARINE -> PRISMARINE_SEED.get();
            case GHAST_TEAR -> GHAST_TEAR_SEED.get();
            case BREEZE_ROD -> BREEZE_ROD_SEED.get();
            case BASIL -> BASIL_SEED.get();
            case MINT -> MINT_SEED.get();
            case ROSEMARY -> ROSEMARY_SEED.get();
            case THYME -> THYME_SEED.get();
            case PARSLEY -> PARSLEY_SEED.get();
            case CORIANDER -> CORIANDER_SEED.get();
            case GINGER -> GINGER_SEED.get();
            case TURMERIC -> TURMERIC_SEED.get();
            case KALE -> KALE_SEED.get();
            case PAK_CHOI -> PAK_CHOI_SEED.get();
            case BRUSSELS_SPROUTS -> BRUSSELS_SPROUTS_SEED.get();
            case SWEDE -> SWEDE_SEED.get();
            case CELERIAC -> CELERIAC_SEED.get();
            case LENTIL -> LENTIL_SEED.get();
            case CHICKPEA -> CHICKPEA_SEED.get();
            case BARLEY -> BARLEY_SEED.get();
            case BUCKWHEAT -> BUCKWHEAT_SEED.get();
            case RHUBARB -> RHUBARB_SEED.get();
            case GRAPE -> GRAPE_SEED.get();
            case CANTALOUPE -> CANTALOUPE_SEED.get();
            case SPINACH -> SPINACH_SEED.get();
            case CAULIFLOWER -> CAULIFLOWER_SEED.get();
            case ARTICHOKE -> ARTICHOKE_SEED.get();
            case CHILLI -> CHILLI_SEED.get();
            case SPRING_ONION -> SPRING_ONION_SEED.get();
            case FENNEL -> FENNEL_SEED.get();
            case DILL -> DILL_SEED.get();
            case PEANUT -> PEANUT_SEED.get();
            case SESAME -> SESAME_SEED.get();
            case OATS -> OATS_SEED.get();
            case GOOSEBERRY -> GOOSEBERRY_SEED.get();
            case BLACKCURRANT -> BLACKCURRANT_SEED.get();
            case PUMPKIN -> PUMPKIN_SEED.get();
            case BUTTERNUT_SQUASH -> BUTTERNUT_SQUASH_SEED.get();
            case COURGETTE -> COURGETTE_SEED.get();
            case TURNIP -> TURNIP_SEED.get();
            case PARSNIP -> PARSNIP_SEED.get();
            case LEEK -> LEEK_SEED.get();
            case CELERY -> CELERY_SEED.get();
            case ASPARAGUS -> ASPARAGUS_SEED.get();
            case BLUEBERRY -> BLUEBERRY_SEED.get();
            case RASPBERRY -> RASPBERRY_SEED.get();
            case BLACKBERRY -> BLACKBERRY_SEED.get();
            case CRANBERRY -> CRANBERRY_SEED.get();
            case CORN -> CORN_SEED.get();
            case PEAS -> PEAS_SEED.get();
            case BEANS -> BEANS_SEED.get();
            case BROCCOLI -> BROCCOLI_SEED.get();
            case CABBAGE -> CABBAGE_SEED.get();
            case RADISH -> RADISH_SEED.get();
            case AUBERGINE -> AUBERGINE_SEED.get();
            case SWEET_POTATO -> SWEET_POTATO_SEED.get();
            case TOMATO -> TOMATO_SEED.get();
            case LETTUCE -> LETTUCE_SEED.get();
            case CUCUMBER -> CUCUMBER_SEED.get();
            case ONION -> ONION_SEED.get();
            case GARLIC -> GARLIC_SEED.get();
            case PEPPER -> PEPPER_SEED.get();
            case STRAWBERRY -> STRAWBERRY_SEED.get();
            case RICE -> RICE_SEED.get();
        };
    }

    public static final Supplier<BlockItem> BOTANICAL_GREENHOUSE =
            ITEMS.registerItem("botanical_greenhouse", props -> new BlockItem(AEBlocks.BOTANICAL_GREENHOUSE.get(), props),
                    new Item.Properties());

    public static final Supplier<BlockItem> BOTANICAL_GENETICS_LABORATORY =
            ITEMS.registerItem("botanical_genetics_laboratory", props -> new BlockItem(AEBlocks.BOTANICAL_GENETICS_LABORATORY.get(), props),
                    new Item.Properties());
    public static final Supplier<Item> GENETIC_SAMPLE =
            ITEMS.registerItem("genetic_sample", GeneticSampleItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<Item> LABORATORY_RESEARCH_UPGRADE =
            ITEMS.registerItem("laboratory_research_upgrade", Item::new, new Item.Properties().stacksTo(16));
    public static final Supplier<Item> LABORATORY_HYBRIDIZATION_UPGRADE =
            ITEMS.registerItem("laboratory_hybridization_upgrade", Item::new, new Item.Properties().stacksTo(16));

    public static final Supplier<BlockItem> ARCANE_FARMLAND =
            ITEMS.registerItem("arcane_farmland", props -> new BlockItem(AEBlocks.ARCANE_FARMLAND.get(), props),
                    new Item.Properties());
    public static final Supplier<BlockItem> CELESTIAL_FARMLAND =
            ITEMS.registerItem("celestial_farmland", props -> new BlockItem(AEBlocks.CELESTIAL_FARMLAND.get(), props),
                    new Item.Properties());

    public static final Supplier<BlockItem> IRRIGATION_SPRINKLER = ITEMS.registerItem("irrigation_sprinkler",
            props -> new IrrigationComponentItem(AEBlocks.IRRIGATION_SPRINKLER.get(), props), new Item.Properties());
    public static final Supplier<BlockItem> IRRIGATION_VALVE = ITEMS.registerItem("irrigation_valve",
            props -> new IrrigationComponentItem(AEBlocks.IRRIGATION_VALVE.get(), props), new Item.Properties());

    public static final Supplier<BlockItem> IRRIGATION_PIPE=ITEMS.registerItem("irrigation_pipe",
            props->new IrrigationComponentItem(AEBlocks.IRRIGATION_PIPE.get(),props),new Item.Properties());
    public static final Supplier<Item> SPANNER=ITEMS.registerItem("spanner",SpannerItem::new,new Item.Properties().stacksTo(1));
    public static final Supplier<Item> JOURNAL=ITEMS.registerItem("journal",BuildingJournalItem::new,new Item.Properties().stacksTo(1));
    private AEItems() {}
}
