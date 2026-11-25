package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Pixel-perfect, responsive two-page field journal for Agri Evolution 0.4.3.
 * Displays a snapshot of the SAME 0.4.0+ ItemStack CUSTOM_DATA; this GUI never
 * modifies world, crop, breeding or discovery data. No menu or network packet
 * is necessary because the existing item already synchronizes its data.
 */
public final class JournalScreen extends Screen {
    private final JournalSnapshot snapshot;
    private final JournalNavigation nav;
    private boolean compact;
    private int left;
    private int top;
    private final com.agrievolution.client.NativeGuiViewport viewport = new com.agrievolution.client.NativeGuiViewport();

    public JournalScreen(ItemStack journal) {
        super(Component.literal(journal.is(com.agrievolution.content.AEItems.JOURNAL.get())?"Building & Machines Journal":"Grandmas Journal"));
        this.snapshot = JournalSnapshot.from(journal);
        this.nav = new JournalNavigation(journal.is(com.agrievolution.content.AEItems.JOURNAL.get()));
    }

    @Override
    protected void init() {
        super.init();
        // GUI dimensions are *already* scaled by Minecraft. Do not scale the
        // PoseStack by a fractional number: that rasterizes both art and font
        // between pixel boundaries (the blur in 0.4.2 screenshots).
        compact = width < JournalTheme.WIDTH + 12 || height < JournalTheme.HEIGHT + 12;
        int w = compact ? CompactJournalRenderer.WIDTH : JournalTheme.WIDTH;
        int h = compact ? CompactJournalRenderer.HEIGHT : JournalTheme.HEIGHT;
        viewport.init(width, height, w, h);
        left = viewport.x();
        top = viewport.y();
    }

    private int localX(double screenX) { return (int) Math.floor(screenX - left); }
    private int localY(double screenY) { return (int) Math.floor(screenY - top); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // IMPORTANT (1.21.1): Screen.render() invokes renderBackground(), and
        // renderBackground() runs the game's framebuffer blur shader. Call it
        // BEFORE drawing anything belonging to this journal. Calling it last
        // blurs the entire journal, including Font glyphs and 32x32 icons.
        super.render(gui, mouseX, mouseY, partialTick);

        gui.fill(0, 0, width, height, 0xB4000806);
        int mx = localX(mouseX);
        int my = localY(mouseY);
        gui.pose().pushPose();
        gui.pose().translate(left, top, 0);
        // No gui.pose().scale(): native integer-aligned pixels and crisp font.
        if (compact) {
            CompactJournalRenderer.render(gui, font, nav, snapshot, mx, my);
        } else {
            JournalRenderer.frame(gui, font, nav, snapshot, mx, my);
            switch (nav.section()) {
                case OVERVIEW -> renderOverview(gui);
                case SPECIES -> renderSpecies(gui, mx, my);
                case BREEDING -> renderBreeding(gui);
                case NOTES -> renderNotes(gui);
                case MACHINES -> renderMachines(gui,mx,my);
                case TIPS -> JournalIllustrations.tips(gui,font,nav.tipsPage(),false);
            }
        }
        int indexY=compact?CompactJournalRenderer.HEIGHT-18:JournalTheme.HEIGHT-22;
        com.agrievolution.client.GardenGuiTheme.panel(gui,compact?205:226,indexY,compact?160:178,16,0xFF275841);
        gui.drawString(font,"I: Contents / Search",compact?212:234,indexY+4,0xFFF0D0,false);
        gui.pose().popPose();

        if (viewport.oversized()) {
            gui.fill(0, height - 14, width, height, 0xEF281D17);
            gui.drawCenteredString(font, "Ctrl+scroll or arrows  |  Shift: sideways", width / 2, height - 11, 0xFFF0DEB8);
        }

        if (nav.section() == JournalNavigation.Section.SPECIES) {
            for (int row = 0; row < JournalNavigation.SPECIES_PER_PAGE; row++) {
                int i = nav.speciesPageStart() + row;
                if (i >= CropSpecies.values().length) break;
                boolean hovered = compact
                        ? CompactJournalRenderer.speciesHit(mx, my, row)
                        : JournalRenderer.inside(mx, my, 36, 102 + 34 * row, 176, 31);
                if (hovered) {
                    CropSpecies species = CropSpecies.values()[i];
                    boolean known = snapshot.entry(species).discovered();
                    gui.renderTooltip(font, Component.literal(known ? species.displayName()
                            : "Undiscovered specimen"), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void title(GuiGraphics gui, String leftTitle, String rightTitle) {
        JournalRenderer.heading(gui, font, leftTitle, 38, 85, JournalTheme.GREEN);
        JournalRenderer.heading(gui, font, rightTitle, 228, 85, JournalTheme.GREEN);
    }

    private void text(GuiGraphics gui, String text, int x, int y, int colour) {
        gui.drawString(font, JournalFonts.body(text), x, y, colour, false);
    }

    private void renderOverview(GuiGraphics gui) {
        title(gui, "FIELD RESEARCH", "THE BOTANICAL COLLECTION");
        text(gui, "Research journal", 38, 111, JournalTheme.INK);
        text(gui, "Species found: " + snapshot.discoveredCount() + " / " + snapshot.speciesCount(),
                38, 130, JournalTheme.INK);
        JournalRenderer.progress(gui, 38, 148, 153,
                snapshot.discoveredCount(), snapshot.speciesCount(), JournalTheme.GREEN);
        text(gui, snapshot.discoveredCount() * 100 / snapshot.speciesCount() + "% COMPLETE",
                38, 160, JournalTheme.GREEN);
        text(gui, "1. Start Dirt + Wood", 38, 187, JournalTheme.GREEN);
        text(gui, "2. Follow Breeding tab", 38, 202, JournalTheme.INK);
        text(gui, "3. Read Instructions", 38, 217, JournalTheme.INK);

        CropSpecies featured = CropSpecies.VIOLET_STARFLOWER;
        boolean discovered = snapshot.entry(featured).discovered();
        JournalRenderer.speciesIcon(gui, featured, discovered, 236, 113, 2);
        text(gui, discovered ? "VIOLET STARFLOWER" : "MYSTERY HYBRID",
                304, 119, discovered ? JournalTheme.PURPLE : JournalTheme.LOCKED);
        text(gui, discovered ? "Rare mutation found" : "Undiscovered", 304, 136, JournalTheme.INK);
        text(gui, "All flower and ore", 230, 187, JournalTheme.INK);
        text(gui, "pairs are listed in", 230, 202, JournalTheme.INK);
        text(gui, "Breeding (use arrows).", 230, 217, JournalTheme.GREEN);
    }

    private void renderSpecies(GuiGraphics gui, int mx, int my) {
        title(gui, "SPECIES INDEX", "SPECIMEN RECORD");
        CropSpecies[] species = CropSpecies.values();
        int start = nav.speciesPageStart();
        text(gui, nav.speciesPageNumber() + "/" + nav.speciesPageCount(), 180, 84, JournalTheme.MUTED);
        for (int row = 0; row < JournalNavigation.SPECIES_PER_PAGE && start + row < species.length; row++) {
            int i = start + row;
            int y = 101 + row * 34;
            JournalSnapshot.Entry entry = snapshot.entry(species[i]);
            if (i == nav.speciesIndex()) {
                gui.fill(35, y, 210, y + 32, 0x3D39764B);
                gui.fill(35, y, 38, y + 32, 0xFF598C59);
            } else if (JournalRenderer.inside(mx, my, 36, y, 173, 32)) {
                gui.fill(35, y, 210, y + 32, 0x1E6B543A);
            }
            JournalRenderer.speciesIcon(gui, species[i], entry.discovered(), 39, y, 1);
            String label = entry.discovered() ? species[i].displayName() : "??? Unknown";
            text(gui, label, 76, y + 7, entry.discovered() ? JournalTheme.INK : JournalTheme.LOCKED);
            text(gui, entry.discovered() ? (species[i].isMutation() ? "RARE / RECORDED" : "RECORDED")
                    : "LOCKED", 76, y + 20, entry.discovered() ? JournalTheme.GREEN : JournalTheme.MUTED);
        }

        CropSpecies chosen = nav.selectedSpecies();
        JournalSnapshot.Entry record = snapshot.entry(chosen);
        JournalRenderer.speciesIcon(gui, chosen, record.discovered(), 234, 111, 2);
        if (!record.discovered()) {
            text(gui, "UNKNOWN SPECIES", 301, 117, JournalTheme.LOCKED);
            text(gui, "Not discovered", 301, 134, JournalTheme.MUTED);
            text(gui, "Inspect a growing", 232, 186, JournalTheme.INK);
            text(gui, "crop with the journal", 232, 199, JournalTheme.INK);
            text(gui, "to reveal this entry.", 232, 212, JournalTheme.INK);
        } else {
            text(gui, chosen.displayName(), 303, 113,
                    chosen.isMutation() ? JournalTheme.PURPLE : JournalTheme.GREEN);
            text(gui, chosen.isMutation() ? "RARE MUTATION" : "DISCOVERED", 303, 130,
                    chosen.isMutation() ? JournalTheme.PURPLE : JournalTheme.MUTED);
            text(gui, "Harvest: " + harvest(chosen), 232, 184, JournalTheme.INK);
            if (record.hasGenetics()) {
                stat(gui, "Growth", record.growth(), 199, JournalTheme.GREEN);
                stat(gui, "Yield", record.yield(), 214, JournalTheme.GOLD);
                stat(gui, "Resilience", record.resilience(), 229, JournalTheme.BLUE);
            } else {
                text(gui, "Genes not yet observed", 232, 210, JournalTheme.MUTED);
            }
        }
        JournalRenderer.sprite(gui, "arrow_left", 357, 177, 20, 20);
        JournalRenderer.sprite(gui, "arrow_right", 380, 177, 20, 20);
    }

    private void stat(GuiGraphics gui, String name, int value, int y, int colour) {
        text(gui, name, 232, y, JournalTheme.INK);
        JournalRenderer.progress(gui, 291, y + 1, 79, value, 100, colour);
        text(gui, Integer.toString(value), 378, y, colour);
    }

    private static String harvest(CropSpecies species) {
        return switch (species) {
            case WILD -> "Wheat";
            case CRIMSON_CLOVER, AZURE_FLAX, VIOLET_STARFLOWER, AMBER_MARIGOLD, SILVER_THISTLE,
                    SUNFIRE_LILY, MOON_ORCHID, ECLIPSE_LOTUS, TWILIGHT_SAGE -> JournalRenderer.flowerHarvestName(species);
            case DIRT -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case WOOD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case STONE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case COAL -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case COPPER -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case IRON -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case REDSTONE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GOLD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case LAPIS -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case DIAMOND -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case EMERALD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case QUARTZ -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GRAVEL -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SAND -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case CLAY -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case FLINT -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GLOWSTONE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case OBSIDIAN -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case NETHERRACK -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SOUL_SAND -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case MAGMA_CREAM -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case NETHER_WART -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case BLAZE_ROD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case END_STONE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case CHORUS_FRUIT -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case ENDER_PEARL -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SHULKER_SHELL -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case ANCIENT_DEBRIS -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case NETHERITE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, DARK_OAK_SAPLING, TUBE_CORAL, BRAIN_CORAL, BUBBLE_CORAL, FIRE_CORAL, COAST_ARMOR_TRIM_SMITHING_TEMPLATE, TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SUGAR_CANE, SWEET_BERRIES, SEAGRASS, HANGING_ROOTS, TORCHFLOWER, PITCHER_PLANT, SPORE_BLOSSOM, BIG_DRIPLEAF, TRIAL_KEY, OMINOUS_TRIAL_KEY, NETHERITE_UPGRADE_SMITHING_TEMPLATE, DISC_FRAGMENT_5 -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GRASS_BLOCK, PODZOL, MYCELIUM, DEAD_BUSH, MUD_BRICKS, DEEPSLATE_BRICKS, CALIBRATED_SCULK_SENSOR, END_CRYSTAL, TOTEM_OF_UNDYING, ENCHANTED_GOLDEN_APPLE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case JUNGLE_LOG, COARSE_DIRT, ROOTED_DIRT, RED_SANDSTONE, PRISMARINE_BRICKS, WARPED_WART_BLOCK, RED_NETHER_BRICKS, SCULK_SENSOR, SCULK_SHRIEKER, CONDUIT, RESPAWN_ANCHOR -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case STICK, DRIED_KELP, GRAY_DYE, LIGHT_GRAY_DYE, SKELETON_SKULL, WITHER_SKELETON_SKULL, CREEPER_HEAD, ZOMBIE_HEAD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case WHITE_DYE, BLACK_DYE, ORANGE_DYE, PINK_DYE, PURPLE_DYE, MAGENTA_DYE, LIGHT_BLUE_DYE, LIME_DYE, HEART_OF_THE_SEA, NETHER_STAR, DRAGON_HEAD, HEAVY_CORE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GREEN_DYE, RED_DYE, YELLOW_DYE, BROWN_DYE, HONEY_BOTTLE, GLOW_BERRIES, BLUE_DYE, CYAN_DYE, FIRE_CHARGE, WIND_CHARGE, DRAGON_BREATH, EXPERIENCE_BOTTLE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SUGAR, COCOA_BEANS, EGG, ROTTEN_FLESH, RABBIT_HIDE, RABBIT_FOOT, ARMADILLO_SCUTE, FERMENTED_SPIDER_EYE, WITHER_ROSE, CHORUS_FLOWER, CRIMSON_FUNGUS, WARPED_FUNGUS -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SPRUCE_LOG, BIRCH_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG, SEA_PICKLE, GLOW_LICHEN, CRIMSON_STEM, WARPED_STEM, VERDANT_FROGLIGHT, PEARLESCENT_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case CHARCOAL, GLASS, BROWN_MUSHROOM, RED_MUSHROOM, MOSSY_COBBLESTONE, LILY_PAD, DARK_PRISMARINE, GILDED_BLACKSTONE, SCULK_CATALYST, OCHRE_FROGLIGHT -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case RED_SAND, SANDSTONE, CACTUS, VINE, PACKED_MUD, BRICK, ICE, PACKED_ICE, BLUE_ICE, NETHER_BRICK, ENDER_EYE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case GRANITE, DIORITE, ANDESITE, MUD, TUFF, DRIPSTONE, DEEPSLATE, SOUL_SOIL, SEA_LANTERN, SHROOMLIGHT, END_ROD, SCULK -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case PAPER, KELP, BAMBOO, BLACKSTONE, BASALT, PURPUR, TURTLE_SCUTE, SPONGE -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case SNOWBALL, MOSS, CALCITE, TERRACOTTA, SPIDER_EYE, GLOW_INK_SAC, CRYING_OBSIDIAN, NAUTILUS_SHELL -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case FEATHER, INK_SAC, HONEYCOMB, SLIME_BALL, GUNPOWDER, PRISMARINE_CRYSTALS, PHANTOM_MEMBRANE, ECHO_SHARD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case STRING, WOOL, LEATHER, BONE, AMETHYST, PRISMARINE, GHAST_TEAR, BREEZE_ROD -> com.agrievolution.farming.ResourceCropRules.produceName(species);
            case BASIL, MINT, ROSEMARY, THYME, PARSLEY, CORIANDER, GINGER, TURMERIC, PUMPKIN, BUTTERNUT_SQUASH, COURGETTE, TURNIP, PARSNIP, LEEK, CELERY, ASPARAGUS, BLUEBERRY, RASPBERRY, BLACKBERRY, CRANBERRY, SPINACH, CAULIFLOWER, ARTICHOKE, CHILLI, SPRING_ONION, FENNEL, DILL, PEANUT, SESAME, OATS, GOOSEBERRY, BLACKCURRANT, KALE, PAK_CHOI, BRUSSELS_SPROUTS, SWEDE, CELERIAC, LENTIL, CHICKPEA, BARLEY, BUCKWHEAT, RHUBARB, GRAPE, CANTALOUPE -> species.displayName();
            case CORN, PEAS, BEANS, BROCCOLI, CABBAGE, RADISH, AUBERGINE, SWEET_POTATO -> species.displayName();
            case TOMATO, LETTUCE, CUCUMBER, ONION, GARLIC, PEPPER, STRAWBERRY, RICE -> species.displayName();
        };
    }

    private void renderBreeding(GuiGraphics gui) {
        BreedingGuidePages.Page page = BreedingGuidePages.get(nav.breedingPage());
        title(gui, page.title(), page.subtitle());
        if(BreedingGuidePages.recipe(nav.breedingPage())!=null)
            JournalIllustrations.recipe(gui,font,BreedingGuidePages.recipe(nav.breedingPage()),false);
        for (int i = 0; i < page.left().length; i++)
            text(gui, page.left()[i], 38, 108 + i * 19, JournalTheme.INK);
        for (int i = 0; i < page.right().length; i++)
            text(gui, page.right()[i], 228, 108 + i * 19, JournalTheme.INK);
        text(gui, "BREEDING " + (nav.breedingPage() + 1) + "/" + nav.breedingPageCount(),
                230, 232, JournalTheme.GREEN);
        JournalRenderer.sprite(gui, "arrow_left", 357, 223, 19, 19);
        JournalRenderer.sprite(gui, "arrow_right", 381, 223, 19, 19);
    }

    private void renderMachines(GuiGraphics gui,int mx,int my) {
        MachineGuidePages.Page page = nav.guidePage();
        title(gui, page.title().startsWith("CRAFT ")?"CRAFTING RECIPE":page.title(), page.subtitle());
        MachineIllustrations.render(gui,font,page.title(),false,mx,my);
        for (int i = 0; i < page.left().length; i++)
            text(gui, page.left()[i], 38, 110 + i * 19, JournalTheme.INK);
        String[] progress = MilestonePage.progress(page.title());
        String[] right = progress == null ? page.right() : progress;
        for (int i = 0; i < right.length; i++)
            text(gui, right[i], 228, 110 + i * 19, JournalTheme.INK);
        text(gui, "INSTRUCTIONS " + (nav.machinePage() + 1) + "/" + nav.machinePageCount(),
                230, 230, JournalTheme.GREEN);
        JournalRenderer.sprite(gui, "arrow_left", 357, 223, 19, 19);
        JournalRenderer.sprite(gui, "arrow_right", 381, 223, 19, 19);
    }

    private void renderNotes(GuiGraphics gui) {
        title(gui, "GENETICS NOTES", "RESEARCH LEDGER");
        text(gui, "GROWTH", 38, 111, JournalTheme.GREEN);
        text(gui, "Higher = faster crops", 38, 124, JournalTheme.INK);
        text(gui, "YIELD", 38, 150, JournalTheme.GOLD);
        text(gui, "Higher = extra harvest", 38, 163, JournalTheme.INK);
        text(gui, "RESILIENCE", 38, 189, JournalTheme.BLUE);
        text(gui, "Hardier crop genetics", 38, 202, JournalTheme.INK);
        text(gui, "Each trait ranges 0-100.", 38, 224, JournalTheme.MUTED);

        text(gui, "RARE TRAITS " + snapshot.rareDiscoveredCount() + "/3", 231, 115, JournalTheme.GREEN);
        text(gui, snapshot.rareDiscovered(RareTrait.VIGOROUS) ? "Vigorous - FOUND" : "??? Growth", 231, 138, JournalTheme.INK);
        text(gui, snapshot.rareDiscovered(RareTrait.ABUNDANT) ? "Abundant - FOUND" : "??? Harvest", 231, 165, JournalTheme.INK);
        text(gui, snapshot.rareDiscovered(RareTrait.TENACIOUS) ? "Tenacious - FOUND" : "??? Nutrition", 231, 192, JournalTheme.INK);
        text(gui, "Inspect rare plants", 231, 216, JournalTheme.MUTED);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = localX(mouseX);
            int my = localY(mouseY);
            int indexY=compact?CompactJournalRenderer.HEIGHT-18:JournalTheme.HEIGHT-22;
            if(JournalRenderer.inside(mx,my,compact?205:226,indexY,compact?160:178,16)){openIndex();return true;}
            for (int i = 0; i < nav.sections().length; i++) {
                boolean clicked = compact
                        ? CompactJournalRenderer.tabHit(mx, my, i,nav.building())
                        : JournalRenderer.inside(mx, my, JournalRenderer.tabX(i), 50, nav.building()?62:58, 16);
                if (clicked) {
                    nav.section(nav.sections()[i]);
                    return true;
                }
            }
            if (nav.section() == JournalNavigation.Section.BREEDING || nav.section() == JournalNavigation.Section.TIPS) {
                boolean previous = compact
                        ? CompactJournalRenderer.guidePreviousHit(mx, my)
                        : JournalRenderer.inside(mx, my, 355, 221, 22, 22);
                boolean next = compact
                        ? CompactJournalRenderer.guideNextHit(mx, my)
                        : JournalRenderer.inside(mx, my, 379, 221, 22, 22);
                if (previous || next) {
                    if(nav.section()==JournalNavigation.Section.TIPS)nav.cycleTips(next?1:-1);else nav.cycleBreedingPage(next ? 1 : -1);
                    return true;
                }
            }
            if (nav.section() == JournalNavigation.Section.MACHINES) {
                boolean previous = compact
                        ? CompactJournalRenderer.machinesPreviousHit(mx, my)
                        : JournalRenderer.inside(mx, my, 355, 221, 22, 22);
                boolean next = compact
                        ? CompactJournalRenderer.machinesNextHit(mx, my)
                        : JournalRenderer.inside(mx, my, 379, 221, 22, 22);
                if (previous || next) {
                    nav.cycleMachinePage(next ? 1 : -1);
                    return true;
                }
            }
            if (nav.section() == JournalNavigation.Section.SPECIES) {
                for (int row = 0; row < JournalNavigation.SPECIES_PER_PAGE; row++) {
                    int index = nav.speciesPageStart() + row;
                    if (index >= CropSpecies.values().length) break;
                    boolean clicked = compact
                            ? CompactJournalRenderer.speciesHit(mx, my, row)
                            : JournalRenderer.inside(mx, my, 35, 101 + row * 34, 175, 32);
                    if (clicked) {
                        nav.selectSpecies(index);
                        return true;
                    }
                }
                boolean previous = compact
                        ? CompactJournalRenderer.previousHit(mx, my)
                        : JournalRenderer.inside(mx, my, 357, 177, 20, 20);
                boolean next = compact
                        ? CompactJournalRenderer.nextHit(mx, my)
                        : JournalRenderer.inside(mx, my, 380, 177, 20, 20);
                if (previous || next) {
                    nav.cycleSpecies(next ? 1 : -1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openIndex(){minecraft.setScreen(new JournalIndexScreen(this,nav,snapshot));}

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (viewport.keyPressed(key, modifiers)) {
            left = viewport.x();
            top = viewport.y();
            return true;
        }
        if(key==73){openIndex();return true;}
        if(nav.building()&&(key==82||key==66)) {
            var pages=MachineGuidePages.forBook(true);int target=0;
            if(key==82)while(target<pages.length&&!pages[target].title().startsWith("CRAFT "))target++;
            if(target<pages.length)nav.cycleMachinePage(target-nav.machinePage());
            return true;
        }
        // GLFW left/right arrows: previous/next specimen or previous/next section.
        if (key == 263 || key == 262) {
            int delta = key == 262 ? 1 : -1;
            if (nav.section() == JournalNavigation.Section.SPECIES) nav.cycleSpecies(delta);
            else if (nav.section() == JournalNavigation.Section.MACHINES) nav.cycleMachinePage(delta);
            else if (nav.section() == JournalNavigation.Section.BREEDING) nav.cycleBreedingPage(delta);
            else if (nav.section() == JournalNavigation.Section.TIPS) nav.cycleTips(delta);
            else nav.cycleSection(delta);
            return true;
        }
        if (key >= 49 && key < 49 + nav.sections().length) { // keyboard 1..5 select sections
            nav.section(nav.sections()[key - 49]);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasControlDown() && viewport.oversized()) {
            viewport.scroll(scrollX, scrollY, hasShiftDown());
            left = viewport.x();
            top = viewport.y();
            return true;
        }
        if (scrollY != 0) {
            if (nav.section() == JournalNavigation.Section.SPECIES) {
                nav.cycleSpecies(scrollY > 0 ? -1 : 1);
                return true;
            }
            if (nav.section() == JournalNavigation.Section.BREEDING || nav.section() == JournalNavigation.Section.TIPS) {
                if(nav.section()==JournalNavigation.Section.TIPS)nav.cycleTips(scrollY>0?-1:1);
                else nav.cycleBreedingPage(scrollY > 0 ? -1 : 1);
                return true;
            }
            if (nav.section() == JournalNavigation.Section.MACHINES) {
                nav.cycleMachinePage(scrollY > 0 ? -1 : 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}



