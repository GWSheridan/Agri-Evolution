package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Native-pixel 380x204 layout for high Minecraft GUI scales.
 * IMPORTANT: Never scale GuiGraphics pose or font here. Cropping a few decorative
 * pixels is preferable to blurring every glyph with a fractional transform.
 */
public final class CompactJournalRenderer {
    private CompactJournalRenderer() {}
    public static final int WIDTH = 380;
    public static final int HEIGHT = 204;
    public static final int TAB_LEFT = 12;
    public static final int TAB_TOP = 35;
    public static final int TAB_STEP = 60;
    public static final int TAB_WIDTH = 55;
    public static final int TAB_HEIGHT = 16;
    public static final int ROW_TOP = 83;
    public static final int ROW_STEP = 24;
    public static final int ROW_HEIGHT = 23;
    public static final int ARROW_LEFT_X = 324;
    public static final int ARROW_RIGHT_X = 346;
    public static final int ARROW_Y = 108;

    private static void text(GuiGraphics g, Font f, String text, int x, int y, int col) {
        g.drawString(f, JournalFonts.body(text), x, y, col, false);
    }

    private static void heading(GuiGraphics g, Font f, String text, int x) {
        g.drawString(f, JournalFonts.body(text), x, 64, JournalTheme.GREEN, false);
        g.fill(x, 77, x + 155, 78, 0xFFBD995D);
    }

    private static void icon(GuiGraphics g, CropSpecies species, boolean known, int x, int y) {
        // Read the approved in-game crop texture.
        if (known) { JournalRenderer.currentCrop(g, species, x, y, 16); return; }
        String s = "undiscovered";
        g.blit(JournalTheme.texture("species/small/" + s), x, y, 0, 0, 16, 16, 16, 16);
    }

    private static void progress(GuiGraphics g, int x, int y, int w, int value, int max, int colour) {
        JournalRenderer.progress(g, x, y, w, value, max, colour);
    }

    public static boolean tabHit(int mx, int my, int i) {
        return JournalRenderer.inside(mx, my, TAB_LEFT + i * TAB_STEP, TAB_TOP, TAB_WIDTH, TAB_HEIGHT);
    }
    public static boolean tabHit(int mx,int my,int i,boolean building){
        return building?JournalRenderer.inside(mx,my,TAB_LEFT+i*73,TAB_TOP,64,TAB_HEIGHT):tabHit(mx,my,i);
    }

    public static boolean speciesHit(int mx, int my, int i) {
        return JournalRenderer.inside(mx, my, 20, ROW_TOP + i * ROW_STEP, 164, ROW_HEIGHT);
    }

    public static boolean previousHit(int mx, int my) {
        return JournalRenderer.inside(mx, my, ARROW_LEFT_X, ARROW_Y, 20, 20);
    }

    public static boolean nextHit(int mx, int my) {
        return JournalRenderer.inside(mx, my, ARROW_RIGHT_X, ARROW_Y, 20, 20);
    }

    public static void render(GuiGraphics g, Font f, JournalNavigation nav,
                              JournalSnapshot data, int mx, int my) {
        JournalRenderer.sprite(g, "journal_background_compact", 0, 0, WIDTH, HEIGHT);
        text(g, f, "AGRI EVOLUTION", 22, 16, JournalTheme.GOLD);
        text(g, f, nav.building()?"BUILDING & MACHINES":"GRANDMAS JOURNAL", 214, 16, 0xFFE7D5A5);
        JournalNavigation.Section[] sections = nav.sections();
        for (int i = 0; i < sections.length; i++) {
            int x = TAB_LEFT + i * (nav.building()?73:TAB_STEP);
            int tabWidth=nav.building()?64:TAB_WIDTH;
            boolean active = nav.section() == sections[i];
            JournalRenderer.sprite(g, active ? "tab_active_compact" : "tab_inactive_compact",
                    x, TAB_TOP, tabWidth, TAB_HEIGHT);
            var tabText = JournalFonts.body(nav.sectionTitle(sections[i]));
            g.drawString(f, tabText, x + tabWidth / 2 - f.width(tabText) / 2, TAB_TOP + 4,
                    active ? JournalTheme.INK : (tabHit(mx, my, i,nav.building()) ? 0xFFFBDA : 0xE5D4A9), false);
        }
        text(g, f, nav.building()?"BUILDING & MACHINE GUIDE":"DISCOVERED " + data.discoveredCount() + "/" + data.speciesCount(),
                20, 190, JournalTheme.GREEN);
        text(g, f, nav.building()?"R: recipes  B: builds":"Agri Evolution / Field Notes", 206, 190, JournalTheme.MUTED);

        switch (nav.section()) {
            case OVERVIEW -> overview(g, f, data);
            case SPECIES -> species(g, f, nav, data, mx, my);
            case BREEDING -> breeding(g, f, nav);
            case NOTES -> notes(g, f, data);
            case MACHINES -> machines(g, f, nav,mx,my);
            case TIPS -> JournalIllustrations.tips(g,f,nav.tipsPage(),true);
        }
    }

    private static void overview(GuiGraphics g, Font f, JournalSnapshot data) {
        heading(g, f, "FIELD RESEARCH", 22);
        heading(g, f, "THE COLLECTION", 200);
        text(g, f, "Discovered: " + data.discoveredCount() + " / " + data.speciesCount(),
                22, 88, JournalTheme.INK);
        progress(g, 22, 105, 153, data.discoveredCount(), data.speciesCount(), JournalTheme.GREEN);
        text(g, f, data.discoveredCount() * 100 / Math.max(1, data.speciesCount()) + "% COMPLETE",
                22, 117, JournalTheme.GREEN);
        text(g, f, "Start Dirt + Wood", 22, 144, JournalTheme.GREEN);
        text(g, f, "See Breeding recipes", 22, 156, JournalTheme.INK);
        boolean known = data.entry(CropSpecies.VIOLET_STARFLOWER).discovered();
        JournalRenderer.speciesIcon(g, CropSpecies.VIOLET_STARFLOWER, known, 205, 84, 1);
        text(g, f, known ? "Violet Starflower" : "Unknown Hybrid", 244, 89,
                known ? JournalTheme.PURPLE : JournalTheme.LOCKED);
        text(g, f, known ? "Rare mutation" : "Undiscovered", 244, 103, JournalTheme.MUTED);
        text(g, f, "All recipes in", 202, 143, JournalTheme.INK);
        text(g, f, "Breeding tab. Use", 202, 156, JournalTheme.INK);
        text(g, f, "arrows for pages.", 202, 169, JournalTheme.GREEN);
    }

    private static void species(GuiGraphics g, Font f, JournalNavigation nav,
                                JournalSnapshot data, int mx, int my) {
        heading(g, f, "SPECIES INDEX", 22);
        heading(g, f, "SPECIMEN RECORD", 200);
        CropSpecies[] plants = CropSpecies.values();
        int start = nav.speciesPageStart();
        text(g, f, nav.speciesPageNumber() + "/" + nav.speciesPageCount(), 177, 64, JournalTheme.MUTED);
        for (int row = 0; row < JournalNavigation.SPECIES_PER_PAGE && start + row < plants.length; row++) {
            int i = start + row;
            int y = ROW_TOP + row * ROW_STEP;
            boolean known = data.entry(plants[i]).discovered();
            if (nav.speciesIndex() == i) {
                g.fill(19, y - 1, 185, y + 22, 0x584B8655);
                g.fill(19, y - 1, 21, y + 22, 0xFF427E50);
            } else if (speciesHit(mx, my, row)) {
                g.fill(19, y - 1, 185, y + 22, 0x284B8655);
            }
            icon(g, plants[i], known, 24, y + 2);
            text(g, f, known ? plants[i].displayName() : "??? Unknown",
                    46, y + 2, known ? JournalTheme.INK : JournalTheme.LOCKED);
            text(g, f, known ? (plants[i].isMutation() ? "RARE / FOUND" : "RECORDED") : "LOCKED",
                    46, y + 13, known ? JournalTheme.GREEN : JournalTheme.MUTED);
        }
        CropSpecies selected = nav.selectedSpecies();
        JournalSnapshot.Entry entry = data.entry(selected);
        JournalRenderer.speciesIcon(g, selected, entry.discovered(), 202, 83, 1);
        if (entry.discovered()) {
            text(g, f, selected.displayName(), 240, 85,
                    selected.isMutation() ? JournalTheme.PURPLE : JournalTheme.GREEN);
            text(g, f, selected.isMutation() ? "RARE MUTATION" : "DISCOVERED", 240, 98,
                    JournalTheme.MUTED);
            text(g, f, "Harvest: " + harvest(selected), 202, 125, JournalTheme.INK);
            if (entry.hasGenetics()) {
                statistic(g, f, "Growth", entry.growth(), 139, JournalTheme.GREEN);
                statistic(g, f, "Yield", entry.yield(), 153, JournalTheme.GOLD);
                statistic(g, f, "Resilience", entry.resilience(), 167, JournalTheme.BLUE);
            } else {
                text(g, f, "Genes not recorded", 202, 148, JournalTheme.MUTED);
            }
        } else {
            text(g, f, "UNKNOWN SPECIES", 240, 87, JournalTheme.LOCKED);
            text(g, f, "Not recorded", 240, 99, JournalTheme.MUTED);
            text(g, f, "Inspect a crop with", 202, 143, JournalTheme.INK);
            text(g, f, "Grandmas Journal", 202, 156, JournalTheme.INK);
            text(g, f, "to unlock this page.", 202, 169, JournalTheme.MUTED);
        }
        JournalRenderer.sprite(g, "arrow_left", ARROW_LEFT_X, ARROW_Y, 20, 20);
        JournalRenderer.sprite(g, "arrow_right", ARROW_RIGHT_X, ARROW_Y, 20, 20);
    }

    private static String harvest(CropSpecies plant) {
        return switch (plant) {
            case WILD -> "Wheat";
            case CRIMSON_CLOVER, AZURE_FLAX, VIOLET_STARFLOWER, AMBER_MARIGOLD, SILVER_THISTLE,
                    SUNFIRE_LILY, MOON_ORCHID, ECLIPSE_LOTUS, TWILIGHT_SAGE -> JournalRenderer.flowerHarvestName(plant);
            case DIRT -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case WOOD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case STONE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case COAL -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case COPPER -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case IRON -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case REDSTONE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GOLD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case LAPIS -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case DIAMOND -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case EMERALD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case QUARTZ -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GRAVEL -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SAND -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case CLAY -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case FLINT -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GLOWSTONE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case NETHERRACK -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SOUL_SAND -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case MAGMA_CREAM -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case NETHER_WART -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case BLAZE_ROD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case END_STONE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case CHORUS_FRUIT -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case ENDER_PEARL -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SHULKER_SHELL -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case ANCIENT_DEBRIS -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, DARK_OAK_SAPLING, TUBE_CORAL, BRAIN_CORAL, BUBBLE_CORAL, FIRE_CORAL, COAST_ARMOR_TRIM_SMITHING_TEMPLATE, TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SUGAR_CANE, SWEET_BERRIES, SEAGRASS, HANGING_ROOTS, TORCHFLOWER, PITCHER_PLANT, SPORE_BLOSSOM, BIG_DRIPLEAF, TRIAL_KEY, OMINOUS_TRIAL_KEY, NETHERITE_UPGRADE_SMITHING_TEMPLATE, DISC_FRAGMENT_5 -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GRASS_BLOCK, PODZOL, MYCELIUM, DEAD_BUSH, MUD_BRICKS, DEEPSLATE_BRICKS, CALIBRATED_SCULK_SENSOR, END_CRYSTAL, TOTEM_OF_UNDYING, ENCHANTED_GOLDEN_APPLE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case JUNGLE_LOG, COARSE_DIRT, ROOTED_DIRT, RED_SANDSTONE, PRISMARINE_BRICKS, WARPED_WART_BLOCK, RED_NETHER_BRICKS, SCULK_SENSOR, SCULK_SHRIEKER, CONDUIT, RESPAWN_ANCHOR -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case STICK, DRIED_KELP, GRAY_DYE, LIGHT_GRAY_DYE, SKELETON_SKULL, WITHER_SKELETON_SKULL, CREEPER_HEAD, ZOMBIE_HEAD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case WHITE_DYE, BLACK_DYE, ORANGE_DYE, PINK_DYE, PURPLE_DYE, MAGENTA_DYE, LIGHT_BLUE_DYE, LIME_DYE, HEART_OF_THE_SEA, NETHER_STAR, DRAGON_HEAD, HEAVY_CORE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GREEN_DYE, RED_DYE, YELLOW_DYE, BROWN_DYE, HONEY_BOTTLE, GLOW_BERRIES, BLUE_DYE, CYAN_DYE, FIRE_CHARGE, WIND_CHARGE, DRAGON_BREATH, EXPERIENCE_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SUGAR, COCOA_BEANS, EGG, ROTTEN_FLESH, RABBIT_HIDE, RABBIT_FOOT, ARMADILLO_SCUTE, FERMENTED_SPIDER_EYE, WITHER_ROSE, CHORUS_FLOWER, CRIMSON_FUNGUS, WARPED_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SPRUCE_LOG, BIRCH_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG, SEA_PICKLE, GLOW_LICHEN, CRIMSON_STEM, WARPED_STEM, VERDANT_FROGLIGHT, PEARLESCENT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case CHARCOAL, GLASS, BROWN_MUSHROOM, RED_MUSHROOM, MOSSY_COBBLESTONE, LILY_PAD, DARK_PRISMARINE, GILDED_BLACKSTONE, SCULK_CATALYST, OCHRE_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case RED_SAND, SANDSTONE, CACTUS, VINE, PACKED_MUD, BRICK, ICE, PACKED_ICE, BLUE_ICE, NETHER_BRICK, ENDER_EYE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case GRANITE, DIORITE, ANDESITE, MUD, TUFF, DRIPSTONE, DEEPSLATE, SOUL_SOIL, SEA_LANTERN, SHROOMLIGHT, END_ROD, SCULK -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case PAPER, KELP, BAMBOO, BLACKSTONE, BASALT, PURPUR, TURTLE_SCUTE, SPONGE -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case SNOWBALL, MOSS, CALCITE, TERRACOTTA, SPIDER_EYE, GLOW_INK_SAC, CRYING_OBSIDIAN, NAUTILUS_SHELL -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case FEATHER, INK_SAC, HONEYCOMB, SLIME_BALL, GUNPOWDER, PRISMARINE_CRYSTALS, PHANTOM_MEMBRANE, ECHO_SHARD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case STRING, WOOL, LEATHER, BONE, AMETHYST, PRISMARINE, GHAST_TEAR, BREEZE_ROD -> com.agrievolution.farming.ResourceCropRules.produceName(plant);
            case BASIL, MINT, ROSEMARY, THYME, PARSLEY, CORIANDER, GINGER, TURMERIC, PUMPKIN, BUTTERNUT_SQUASH, COURGETTE, TURNIP, PARSNIP, LEEK, CELERY, ASPARAGUS, BLUEBERRY, RASPBERRY, BLACKBERRY, CRANBERRY, SPINACH, CAULIFLOWER, ARTICHOKE, CHILLI, SPRING_ONION, FENNEL, DILL, PEANUT, SESAME, OATS, GOOSEBERRY, BLACKCURRANT, KALE, PAK_CHOI, BRUSSELS_SPROUTS, SWEDE, CELERIAC, LENTIL, CHICKPEA, BARLEY, BUCKWHEAT, RHUBARB, GRAPE, CANTALOUPE -> plant.displayName();
            case CORN, PEAS, BEANS, BROCCOLI, CABBAGE, RADISH, AUBERGINE, SWEET_POTATO -> plant.displayName();
            case TOMATO, LETTUCE, CUCUMBER, ONION, GARLIC, PEPPER, STRAWBERRY, RICE -> plant.displayName();
        };
    }

    private static void statistic(GuiGraphics g, Font f, String label, int val, int y, int col) {
        text(g, f, label, 202, y, JournalTheme.INK);
        progress(g, 267, y + 1, 59, val, 100, col);
        text(g, f, Integer.toString(val), 336, y, col);
    }

    private static void breeding(GuiGraphics g, Font f, JournalNavigation nav) {
        BreedingGuidePages.Page page = BreedingGuidePages.get(nav.breedingPage());
        heading(g, f, page.title().startsWith("CRAFT ")?"CRAFTING RECIPE":page.title(), 22);
        heading(g, f, page.subtitle(), 202);
        if(BreedingGuidePages.recipe(nav.breedingPage())!=null)JournalIllustrations.recipe(g,f,BreedingGuidePages.recipe(nav.breedingPage()),true);
        for (int i = 0; i < page.left().length; i++)
            text(g, f, page.left()[i], 22, 85 + i * 16, JournalTheme.INK);
        String[] progress = MilestonePage.progress(page.title());
        String[] right = progress == null ? page.right() : progress;
        for (int i = 0; i < right.length; i++)
            text(g, f, right[i], 202, 85 + i * 16, JournalTheme.INK);
        text(g, f, "BREEDING " + (nav.breedingPage() + 1) + "/" + nav.breedingPageCount(),
                202, 177, JournalTheme.GREEN);
        JournalRenderer.sprite(g, "arrow_left", 323, 171, 14, 14);
        JournalRenderer.sprite(g, "arrow_right", 347, 171, 14, 14);
    }

    public static boolean guidePreviousHit(int mx, int my) {
        return machinesPreviousHit(mx, my);
    }
    public static boolean guideNextHit(int mx, int my) {
        return machinesNextHit(mx, my);
    }

    private static void machines(GuiGraphics g, Font f, JournalNavigation nav,int mx,int my) {
        MachineGuidePages.Page page = nav.guidePage();
        MachineIllustrations.render(g,f,page.title(),true,mx,my);
        heading(g, f, page.title().startsWith("CRAFT ")?"CRAFTING RECIPE":page.title(), 22);
        heading(g, f, page.subtitle(), 202);
        for (int i = 0; i < page.left().length; i++)
            text(g, f, page.left()[i], 22, 85 + i * 16, JournalTheme.INK);
        for (int i = 0; i < page.right().length; i++)
            text(g, f, page.right()[i], 202, 85 + i * 16, JournalTheme.INK);
        text(g, f, "GUIDE " + (nav.machinePage() + 1) + " / " + nav.machinePageCount(),
                202, 176, JournalTheme.GREEN);
        JournalRenderer.sprite(g, "arrow_left", 323, 171, 14, 14);
        JournalRenderer.sprite(g, "arrow_right", 347, 171, 14, 14);
    }
    public static boolean machinesPreviousHit(int mx, int my) {
        return JournalRenderer.inside(mx, my, 321, 169, 19, 18);
    }
    public static boolean machinesNextHit(int mx, int my) {
        return JournalRenderer.inside(mx, my, 345, 169, 19, 18);
    }

    private static void notes(GuiGraphics g, Font f, JournalSnapshot data) {
        heading(g, f, "GENETICS NOTES", 22);
        heading(g, f, "RESEARCH LEDGER", 200);
        text(g, f, "GROWTH", 22, 86, JournalTheme.GREEN);
        text(g, f, "Higher = faster crops", 22, 98, JournalTheme.INK);
        text(g, f, "YIELD", 22, 118, JournalTheme.GOLD);
        text(g, f, "Higher = extra harvest", 22, 130, JournalTheme.INK);
        text(g, f, "RESILIENCE", 22, 150, JournalTheme.BLUE);
        text(g, f, "Hardier plant genetics", 22, 162, JournalTheme.INK);
        text(g, f, "RARE TRAITS " + data.rareDiscoveredCount() + "/3", 202, 86, JournalTheme.GREEN);
        text(g, f, data.rareDiscovered(RareTrait.VIGOROUS) ? "Vigorous - FOUND" : "??? Growth", 202, 105, JournalTheme.INK);
        text(g, f, data.rareDiscovered(RareTrait.ABUNDANT) ? "Abundant - FOUND" : "??? Harvest", 202, 123, JournalTheme.INK);
        text(g, f, data.rareDiscovered(RareTrait.TENACIOUS) ? "Tenacious - FOUND" : "??? Nutrition", 202, 141, JournalTheme.INK);
        text(g, f, "Use journal on rare crop", 202, 163, JournalTheme.MUTED);
        text(g, f, "to record a discovery.", 202, 176, JournalTheme.MUTED);

        /*text(g, f, "Parent A", 202, 86, JournalTheme.MUTED);
        text(g, f, data.parentSelected() ? "SELECTED" : "NOT SELECTED", 202, 100,
                data.parentSelected() ? JournalTheme.GREEN : JournalTheme.RED);
        text(g, f, "Species discovered", 202, 127, JournalTheme.MUTED);
        text(g, f, data.discoveredCount() + " of " + data.speciesCount(), 202, 141, JournalTheme.GREEN);
        text(g, f, "Records saved inside", 202, 163, JournalTheme.INK);
        text(g, f, "this specific journal.", 202, 176, JournalTheme.INK);*/
    }
}



