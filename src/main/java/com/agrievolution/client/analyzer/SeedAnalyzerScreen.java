package com.agrievolution.client.analyzer;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.client.journal.JournalFonts;
import com.agrievolution.content.SpeciesSeedItem;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.content.AEItems;
import com.agrievolution.client.journal.JournalSnapshot;
import com.agrievolution.genetics.SeedBreedingGuide;
import com.agrievolution.genetics.SeedQuality;
import com.agrievolution.genetics.SpeciesBreeder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Crisp, read-only Seed Analyzer. The primary seed is the synchronized held stack;
 * comparison seeds are snapshots of the local player's other inventory slots.
 * No inventory transfers, client-side gene edits, or speculative network packets.
 */
public final class SeedAnalyzerScreen extends Screen {
    private static final int W = 360;
    private static final int H = 216;
    private static final int DARK = 0xFF2D2019;
    private static final int WOOD = 0xFF63442D;
    private static final int GOLD = 0xFFBA9257;
    private static final int PARCHMENT = 0xFFF0E0B8;
    private static final int INK = 0xFF302518;
    private static final int FOREST = 0xFF315D45;
    private static final int MUTED = 0xFF806B56;
    private static final int BLUE = 0xFF406FA5;
    private static final int YELLOW = 0xFFC28B38;

    private enum Page { PROFILE, COMPARE, BREEDING, RESEARCH }

    private final ItemStack sample;
    private final CropSpecies species;
    private final CropGenome genome;
    private final List<ItemStack> otherSeeds;
    private final JournalSnapshot journalRecord;
    private int selectedOther = 0;
    private Page page = Page.PROFILE;
    private int left;
    private int top;

    /** @param heldSlot main-hand hotbar slot to exclude, or -1 for offhand/empty opening. */
    public SeedAnalyzerScreen(ItemStack seed, int heldSlot) {
        super(Component.literal("Agri Evolution Seed Analyzer"));
        this.sample = seed.copy();
        if (seed.getItem() instanceof SpeciesSeedItem seedItem) {
            this.species = seedItem.species();
            this.genome = GenomeStorage.fromSeed(seed);
        } else {
            this.species = null;
            this.genome = null;
        }
        List<ItemStack> found = new ArrayList<>();
        JournalSnapshot record = null;
        if (Minecraft.getInstance().player != null) {
            for (int slot = 0; slot < 36; slot++) {
                if (slot == heldSlot) continue;
                ItemStack other = Minecraft.getInstance().player.getInventory().getItem(slot);
                if (other.getItem() instanceof SpeciesSeedItem) found.add(other.copy());
                if (record == null && other.is(AEItems.BREEDING_JOURNAL.get())) record = JournalSnapshot.from(other);
            }
        }
        this.otherSeeds = List.copyOf(found);
        this.journalRecord = record;
        // Automatically compare with the strongest available *other* seed.
        for (int i = 1; i < otherSeeds.size(); i++) {
            if (SeedQuality.score(GenomeStorage.fromSeed(otherSeeds.get(i))) >
                    SeedQuality.score(GenomeStorage.fromSeed(otherSeeds.get(selectedOther)))) {
                selectedOther = i;
            }
        }
    }

    private ItemStack comparisonSeed() {
        return otherSeeds.isEmpty() ? ItemStack.EMPTY : otherSeeds.get(selectedOther);
    }

    @Override
    protected void init() {
        super.init();
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // Screen.render performs the vanilla world blur FIRST. UI must be rendered AFTER.
        super.render(gui, mouseX, mouseY, delta);
        gui.fill(0, 0, width, height, 0xB2000D0B);
        gui.pose().pushPose();
        gui.pose().translate(left, top, 0);
        panel(gui, 0, 0, W, H, DARK, GOLD);
        panel(gui, 6, 6, W - 12, 37, FOREST, GOLD);
        line(gui, "AGRI EVOLUTION", 17, 11, 0xFFE5C683);
        line(gui, "BOTANICAL SEED ANALYZER", 155, 11, 0xFFF4E5BC);
        nav(gui, "PROFILE", 11, 78, page == Page.PROFILE);
        nav(gui, "COMPARE", 95, 81, page == Page.COMPARE);
        nav(gui, "BREEDING", 181, 87, page == Page.BREEDING);
        nav(gui, "RESEARCH", 273, 77, page == Page.RESEARCH);
        switch (page) {
            case PROFILE -> renderProfile(gui);
            case COMPARE -> renderCompare(gui);
            case BREEDING -> renderBreeding(gui);
            case RESEARCH -> renderResearch(gui);
        }
        gui.pose().popPose();
    }

    private void nav(GuiGraphics gui, String caption, int x, int w, boolean selected) {
        panel(gui, x, 25, w, 14, selected ? PARCHMENT : 0xFF244A39, GOLD);
        line(gui, caption, x + 9, 28, selected ? INK : 0xFFE7D7AD);
    }

    private void renderProfile(GuiGraphics gui) {
        panel(gui, 9, 46, 137, 140, PARCHMENT, WOOD);
        panel(gui, 152, 46, 199, 140, PARCHMENT, WOOD);
        line(gui, "SPECIMEN", 18, 55, FOREST);
        gui.fill(18, 68, 136, 69, GOLD);
        line(gui, "GENETIC PROFILE", 161, 55, FOREST);
        gui.fill(161, 68, 340, 69, GOLD);

        if (genome == null) {
            line(gui, "NO SEED DETECTED", 18, 90, INK);
            line(gui, "Hold any Agri", 18, 109, MUTED);
            line(gui, "Evolution seed", 18, 122, MUTED);
            line(gui, "and right-click.", 18, 135, MUTED);
            line(gui, "READY FOR ANALYSIS", 162, 91, FOREST);
            line(gui, "Seeds are never", 162, 115, INK);
            line(gui, "consumed or altered.", 162, 128, INK);
        } else {
            gui.pose().pushPose();
            gui.pose().translate(23, 83, 0);
            gui.pose().scale(2.0f, 2.0f, 1.0f); // integer-only pixel art scaling
            gui.renderItem(sample, 0, 0);
            gui.pose().popPose();
            line(gui, species.displayName(), 18, 130, INK);
            line(gui, species.isMutation() ? "RARE MUTATION" : "CULTIVATED SPECIES",
                    18, 148, species.isMutation() ? 0xFF813E95 : FOREST);
            line(gui, "SAMPLE PRESERVED", 18, 167, MUTED);
            trait(gui, "GROWTH", genome.growth(), 161, 79, FOREST);
            trait(gui, "YIELD", genome.yield(), 161, 110, YELLOW);
            trait(gui, "RESILIENCE", genome.resilience(), 161, 141, BLUE);
        }
        footer(gui, genome == null ? "Hold a seed and use the analyzer to view its traits"
                : "QUALITY: " + SeedQuality.rating(genome),
                genome == null ? "" : "GENETIC SCORE: " + SeedQuality.score(genome) + "/100");
    }

    private void renderCompare(GuiGraphics gui) {
        panel(gui, 9, 46, 166, 140, PARCHMENT, WOOD);
        panel(gui, 182, 46, 169, 140, PARCHMENT, WOOD);
        line(gui, "SEED A (HELD)", 18, 54, FOREST);
        line(gui, "SEED B", 192, 54, FOREST);
        line(gui, "<", 315, 54, otherSeeds.size() > 1 ? FOREST : MUTED);
        line(gui, ">", 334, 54, otherSeeds.size() > 1 ? FOREST : MUTED);
        gui.fill(18, 68, 164, 69, GOLD);
        gui.fill(192, 68, 340, 69, GOLD);
        renderMiniProfile(gui, sample, species, genome, 18);
        ItemStack other = comparisonSeed();
        CropSpecies otherSpecies = other.getItem() instanceof SpeciesSeedItem item ? item.species() : null;
        CropGenome otherGenes = otherSpecies == null ? null : GenomeStorage.fromSeed(other);
        renderMiniProfile(gui, other, otherSpecies, otherGenes, 192);
        if (genome == null) footer(gui, "Open analyzer with an Agri Evolution seed", "");
        else if (otherGenes == null) footer(gui, "Keep another seed in your inventory to compare", "");
        else {
            String best = SeedBreedingGuide.strongerSeed(genome, otherGenes);
            int gap = Math.abs(SeedQuality.score(genome) - SeedQuality.score(otherGenes));
            footer(gui, best.equals("EVEN") ? "EQUAL OVERALL GENETICS" : "HIGHER QUALITY: SEED " + best + " (+" + gap + ")",
                    "SEED " + (selectedOther + 1) + " OF " + otherSeeds.size());
        }
    }

    private void renderMiniProfile(GuiGraphics gui, ItemStack stack, CropSpecies kind,
                                   CropGenome genes, int x) {
        if (genes == null) {
            line(gui, "NO SECOND SEED", x, 86, INK);
            line(gui, "Bring two different", x, 108, MUTED);
            line(gui, "seed stacks to", x, 122, MUTED);
            line(gui, "compare their genes.", x, 136, MUTED);
            return;
        }
        gui.renderItem(stack, x, 77);
        line(gui, kind.displayName(), x + 19, 78, INK);
        line(gui, SeedQuality.rating(genes) + " " + SeedQuality.score(genes) + "/100", x + 19, 92, FOREST);
        miniTrait(gui, "GROWTH", genes.growth(), x, 114, FOREST);
        miniTrait(gui, "YIELD", genes.yield(), x, 136, YELLOW);
        miniTrait(gui, "RESILIENCE", genes.resilience(), x, 158, BLUE);
    }

    private void miniTrait(GuiGraphics gui, String label, int value, int x, int y, int colour) {
        line(gui, label, x, y, INK);
        line(gui, Integer.toString(value), x + 126, y, colour);
        gui.fill(x, y + 11, x + 146, y + 17, 0xFF6B573F);
        gui.fill(x + 1, y + 12, x + 145, y + 16, 0xFFDDCAA0);
        gui.fill(x + 1, y + 12, x + 1 + 143 * value / 100, y + 16, colour);
    }

    private void renderBreeding(GuiGraphics gui) {
        panel(gui, 9, 46, 166, 140, PARCHMENT, WOOD);
        panel(gui, 182, 46, 169, 140, PARCHMENT, WOOD);
        line(gui, "BREEDING PAIR", 18, 55, FOREST);
        line(gui, "MUTATION OUTLOOK", 192, 55, FOREST);
        if (otherSeeds.size() > 1) {
            line(gui, "<", 315, 54, FOREST);
            line(gui, ">", 334, 54, FOREST);
        }
        gui.fill(18, 68, 164, 69, GOLD);
        gui.fill(192, 68, 340, 69, GOLD);
        ItemStack other = comparisonSeed();
        CropSpecies otherSpecies = other.getItem() instanceof SpeciesSeedItem item ? item.species() : null;
        CropGenome otherGenes = otherSpecies == null ? null : GenomeStorage.fromSeed(other);
        if (genome == null || otherGenes == null) {
            line(gui, "TWO PARENTS NEEDED", 18, 85, INK);
            line(gui, "Hold seed A and", 18, 104, MUTED);
            line(gui, "carry seed B.", 18, 118, MUTED);
            line(gui, "Choose B on COMPARE", 192, 88, INK);
            line(gui, "to see accurate", 192, 105, MUTED);
            line(gui, "breeding predictions.", 192, 119, MUTED);
            footer(gui, "Seeds are not modified by this preview", "");
            return;
        }
        CropGenome middle = SeedBreedingGuide.expected(genome, otherGenes);
        CropGenome low = SeedBreedingGuide.minimum(genome, otherGenes);
        CropGenome high = SeedBreedingGuide.maximum(genome, otherGenes);
        line(gui, "A: " + species.displayName(), 18, 77, INK);
        line(gui, "B: " + otherSpecies.displayName(), 18, 92, INK);
        line(gui, "EXPECTED TRAITS", 18, 111, FOREST);
        line(gui, "G " + low.growth() + "-" + high.growth() + "  Y " + low.yield() + "-" + high.yield(), 18, 127, INK);
        line(gui, "R " + low.resilience() + "-" + high.resilience(), 18, 142, INK);
        line(gui, "Average score: " + SeedQuality.score(middle), 18, 163, FOREST);

        SpeciesBreeder.MutationRecipe recipe = SpeciesBreeder.mutationRecipe(species, otherSpecies);
        if (recipe != null) {
            line(gui, recipe.offspring().displayName(), 192, 79, 0xFF813E95);
            line(gui, "Base: " + recipe.baseChance() + "% chance", 192, 98, INK);
            line(gui, "Wood / Iron / Obsidian:", 192, 115, FOREST);
            line(gui, recipe.chanceWithSupports(5)+"% / "+recipe.chanceWithSupports(8)+"% / "+recipe.chanceWithSupports(12)+"%",192,130,FOREST);
            
            line(gui, "Miss: parent species.", 192, 150, MUTED);
            line(gui, "Focus: " + SeedBreedingGuide.weakestTrait(genome, otherGenes), 192, 169, FOREST);
        } else {
            line(gui, "NO SPECIAL HYBRID", 192, 80, INK);
            line(gui, "Either parent's", 192, 100, INK);
            line(gui, "species is passed", 192, 113, INK);
            line(gui, "to the new seed.", 192, 126, INK);
            line(gui, "Focus: " + SeedBreedingGuide.weakestTrait(genome, otherGenes), 192, 157, FOREST);
        }
        footer(gui, "Breed 2 mature crops with your journal", "Preview only");
    }


    /** Read-only research dossier: consults the same journal custom data and seed lineage
     * used elsewhere in the mod, never inventing discoveries or mutating seed genes. */
    private void renderResearch(GuiGraphics gui) {
        panel(gui, 9, 46, 166, 140, PARCHMENT, WOOD);
        panel(gui, 182, 46, 169, 140, PARCHMENT, WOOD);
        line(gui, "SPECIMEN HISTORY", 18, 55, FOREST);
        line(gui, "RESEARCH NOTES", 191, 55, FOREST);
        gui.fill(18, 68, 164, 69, GOLD);
        gui.fill(191, 68, 340, 69, GOLD);
        if (genome == null || species == null) {
            line(gui, "NO SEED SELECTED", 18, 85, INK);
            line(gui, "Hold an Agri seed", 18, 106, MUTED);
            line(gui, "to inspect history.", 18, 120, MUTED);
            line(gui, "Journal optional", 191, 85, FOREST);
            line(gui, "Carry it to see", 191, 105, MUTED);
            line(gui, "recorded discoveries.", 191, 119, MUTED);
            footer(gui, "Analysis is read-only", "");
            return;
        }
        HybridLineageStorage.Lineage lineage = HybridLineageStorage.fromSeed(sample);
        RareTrait trait = RareTraitStorage.fromSeed(sample);
        line(gui, species.displayName(), 18, 78, INK);
        line(gui, species.isMutation() ? "MUTATION SPECIES" : "BASE SPECIES", 18, 93,
                species.isMutation() ? 0xFF813E95 : FOREST);
        if (lineage == null) {
            line(gui, "No recorded parents", 18, 111, MUTED);
            line(gui, "on this seed", 18, 124, MUTED);
        } else {
            line(gui, "PARENT A", 18, 108, FOREST);
            line(gui, lineage.first().displayName(), 18, 121, INK);
            line(gui, "PARENT B", 18, 139, FOREST);
            line(gui, lineage.second().displayName(), 18, 152, INK);
        }
        line(gui, trait == RareTrait.NONE ? "RARE TRAIT: NONE" : "TRAIT: " + trait.name(),
                18, 169, trait == RareTrait.NONE ? MUTED : 0xFF813E95);

        int quality = SeedQuality.score(genome);
        line(gui, "Quality: " + quality + "/100", 191, 78, INK);
        line(gui, "Rating: " + SeedQuality.rating(genome), 191, 92, FOREST);
        line(gui, "Improve: " + weakestGene(genome), 191, 108, BLUE);
        line(gui, "Use the Genetics Lab", 191, 122, INK);
        line(gui, "to enhance / graft.", 191, 135, INK);
        if (journalRecord == null) {
            line(gui, "Carry a Field Journal", 191, 152, MUTED);
            line(gui, "for recorded species.", 191, 167, MUTED);
            footer(gui, "Gene scores and ancestry verified", "");
        } else {
            boolean known = journalRecord.entry(species).discovered();
            line(gui, "Journal: " + (known ? "DISCOVERED" : "UNRECORDED"), 191, 152,
                    known ? FOREST : MUTED);
            line(gui, "Species: " + journalRecord.discoveredCount() + "/" + journalRecord.speciesCount(),
                    191, 167, INK);
            footer(gui, "Journal discoveries: " + journalRecord.discoveredCount(),
                    "Rare traits: " + journalRecord.rareDiscoveredCount());
        }
    }

    private static String weakestGene(CropGenome genome) {
        if (genome.growth() <= genome.yield() && genome.growth() <= genome.resilience()) return "Growth";
        if (genome.yield() <= genome.resilience()) return "Yield";
        return "Resilience";
    }

    private void trait(GuiGraphics gui, String label, int value, int x, int y, int colour) {
        line(gui, label, x, y, INK);
        line(gui, value + "/100", x + 142, y, colour);
        gui.fill(x, y + 13, x + 177, y + 23, 0xFF6B573F);
        gui.fill(x + 1, y + 14, x + 176, y + 22, 0xFFDDCAA0);
        gui.fill(x + 1, y + 14, x + 1 + 174 * value / 100, y + 22, colour);
    }

    private void footer(GuiGraphics gui, String leftLabel, String rightLabel) {
        panel(gui, 9, 191, W - 18, 17, 0xFFBDA579, WOOD);
        line(gui, leftLabel, 16, 196, FOREST);
        if (!rightLabel.isBlank()) line(gui, rightLabel, 244, 196, INK);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        double x = mouseX - left;
        double y = mouseY - top;
        if (y >= 25 && y < 39) {
            if (x >= 11 && x < 89) { page = Page.PROFILE; return true; }
            if (x >= 95 && x < 176) { page = Page.COMPARE; return true; }
            if (x >= 181 && x < 268) { page = Page.BREEDING; return true; }
            if (x >= 273 && x < 350) { page = Page.RESEARCH; return true; }
        }
        if ((page == Page.COMPARE || page == Page.BREEDING) && otherSeeds.size() > 1
                && y >= 51 && y < 68) {
            if (x >= 310 && x < 327) {
                selectedOther = Math.floorMod(selectedOther - 1, otherSeeds.size());
                return true;
            }
            if (x >= 329 && x < 347) {
                selectedOther = (selectedOther + 1) % otherSeeds.size();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void line(GuiGraphics gui, String text, int x, int y, int colour) {
        gui.drawString(font, JournalFonts.body(text), x, y, colour);
    }

    private static void panel(GuiGraphics gui, int x, int y, int w, int h, int inside, int border) {
        GardenGuiTheme.panel(gui, x, y, w, h, inside);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
