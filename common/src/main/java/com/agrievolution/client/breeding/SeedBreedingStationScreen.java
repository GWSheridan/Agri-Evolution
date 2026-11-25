package com.agrievolution.client.breeding;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.content.SeedBreedingParents;

import com.agrievolution.content.SeedBreedingStationMenu;
import com.agrievolution.content.SpeciesSeedItem;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.SpeciesBreeder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Draws directly at native GUI resolution: no scaled intermediary and no blurred text. */
public final class SeedBreedingStationScreen extends AbstractContainerScreen<SeedBreedingStationMenu> {
    private static final int BACK = 0xFF281E17, BORDER = 0xFFC39A57, FOREST = 0xFF285947;
    private static final int PAPER = 0xFFF1DFB8, TEXT = 0xFF342519, MUTED = 0xFF816A4D;

    public SeedBreedingStationScreen(SeedBreedingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 360;
        imageHeight = 230;
        titleLabelX = 13;
        titleLabelY = 8;
        inventoryLabelX = 97;
        inventoryLabelY = 119;
    }

    private void panel(GuiGraphics gui, int x, int y, int w, int h, int fill, int border) {
        GardenGuiTheme.panel(gui, x, y, w, h, fill);
    }

    private void line(GuiGraphics gui, String s, int x, int y, int c) {
        gui.drawString(font, s, x, y, c, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(gui, x, y, imageWidth, imageHeight, BACK, BORDER);
        panel(gui, x + 6, y + 6, imageWidth - 12, 25, FOREST, BORDER);
        line(gui, "AGRI EVOLUTION", x + 13, y + 13, PAPER);
        line(gui, "BOTANICAL BREEDING STATION", x + 151, y + 13, PAPER);
        panel(gui, x + 8, y + 35, 344, 75, PAPER, BORDER);
        line(gui, "PARENT A", x + 23, y + 40, TEXT);
        line(gui, "PARENT B", x + 74, y + 40, TEXT);
        line(gui, "FEED", x + 143, y + 40, TEXT);
        line(gui, "SUPPORT", x + 187, y + 40, TEXT);
        line(gui, "OFFSPRING", x + 274, y + 40, FOREST);
        for (int sx : new int[]{41,81,152,192,296}) {
            panel(gui, x + sx - 1, y + 56, 18, 18, 0xFFBBA67D, MUTED);
        }
        line(gui, "+", x + 68, y + 60, TEXT);
        line(gui, "=", x + 267, y + 60, TEXT);
        String predicted = prediction();
        line(gui, predicted, x + 13, y + 87, predicted.startsWith("Mutation") ? 0xFF78468F : TEXT);
        panel(gui, x + 8, y + 111, 344, 12, 0xFFDED1AC, BORDER);
        int length = 336 * Math.min(menu.progress(), menu.maxProgress()) / menu.maxProgress();
        gui.fill(x + 12, y + 114, x + 12 + length, y + 120, FOREST);
        line(gui, "Breeding: " + (100 * menu.progress() / menu.maxProgress()) + "%", x + 223, y + 87, FOREST);
        line(gui, "INVENTORY", x + 13, y + 137, PAPER);
        panel(gui, x + 91, y + 127, 171, 95, 0xFFB79B71, BORDER);
        for (int iy = 0; iy < 3; iy++) for (int ix = 0; ix < 9; ix++)
            panel(gui, x + 95 + ix * 18, y + 131 + iy * 18, 18, 18, 0xFFDDCEAD, MUTED);
        for (int ix = 0; ix < 9; ix++)
            panel(gui, x + 95 + ix * 18, y + 201, 18, 18, 0xFFDDCEAD, MUTED);
        if (nativeParent(menu.parentA()) || nativeParent(menu.parentB())) {
            line(gui, "Native seeds", x + 13, y + 155, PAPER);
            line(gui, "Keep parents", x + 13, y + 173, PAPER);
            line(gui, "Use compost", x + 13, y + 191, PAPER);
            line(gui, "1 seed / cycle", x + 13, y + 207, PAPER);
        } else {
            line(gui, "No fuel", x + 13, y + 155, PAPER);
            line(gui, "Compost boost", x + 13, y + 173, PAPER);
            line(gui, "2 supports:", x + 13, y + 191, PAPER);
            line(gui, "+5/8/12% tier", x + 13, y + 207, PAPER);
        }
    }

    private String prediction() {
        ItemStack a = menu.parentA(), b = menu.parentB();
        return switch (SeedBreedingParents.pair(a, b)) {
            case INCOMPLETE -> "Insert two parent seeds to begin";
            case INVALID -> "Insert valid parent seeds";
            case MIXED -> "Pair native seeds with native seeds";
            case DIFFERENT_SEEDS -> "Use two seeds of the same type";
            case DIFFERENT_VARIANTS -> "Use matching seed variants";
            case NATIVE -> menu.hasOffspring() ? "Collect the offspring to continue"
                    : menu.hasCompost() ? "One matching seed per compost" : "Add Nutrient Compost";
            case AGRI -> agriPrediction((SpeciesSeedItem) a.getItem(), (SpeciesSeedItem) b.getItem());
        };
    }

    private static boolean nativeParent(ItemStack stack) {
        return !stack.isEmpty() && !(stack.getItem() instanceof SpeciesSeedItem);
    }

    private String agriPrediction(SpeciesSeedItem a, SpeciesSeedItem b) {
        CropSpecies x = a.species(), y = b.species();
        SpeciesBreeder.MutationRecipe recipe = SpeciesBreeder.mutationRecipe(x, y);
        if (recipe == null) return "Child inherits a parent species";
        int chance = recipe.chanceWithSupports(menu.bonus());
        return "Mutation: " + recipe.offspring().displayName() + " (" + chance + "%)";
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // All typography is drawn in renderBg at native scale; suppress vanilla title overlay.
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
        super.render(gui, mouseX, mouseY, partial);
        renderTooltip(gui, mouseX, mouseY);
    }
}
