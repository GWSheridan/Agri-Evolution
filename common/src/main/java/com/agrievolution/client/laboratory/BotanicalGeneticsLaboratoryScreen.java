package com.agrievolution.client.laboratory;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.BotanicalGeneticsLaboratoryMenu;
import com.agrievolution.content.GeneticSampleItem;
import com.agrievolution.content.SpeciesSeedItem;
import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.AdvancedHybridBreeder;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Native pixel-perfect rendering in the same palette as the seed analyzer. */
public final class BotanicalGeneticsLaboratoryScreen extends AbstractContainerScreen<BotanicalGeneticsLaboratoryMenu> {
    private static final int DARK = 0xFF281F18, GOLD = 0xFFCBA66A, GREEN = 0xFF285944;
    private static final int PAPER = 0xFFF0DFB8, INK = 0xFF35271C, SOFT = 0xFF8B755A;
    private static final String[] MODES = {"GROWTH", "YIELD", "RESIST", "EXTRACT", "GRAFT", "EVOLVE", "HYBRID"};

    public BotanicalGeneticsLaboratoryScreen(BotanicalGeneticsLaboratoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 396;
        imageHeight = 245;
        inventoryLabelX = 13;
        inventoryLabelY = 136;
    }
    private void rectangle(GuiGraphics graphics, int x, int y, int w, int h, int border, int fill) {
        GardenGuiTheme.panel(graphics, x, y, w, h, fill);
    }
    private void text(GuiGraphics graphics, String s, int x, int y, int color) {
        graphics.drawString(font, s, x, y, color, false);
    }
    private void progress(GuiGraphics graphics, int x, int y, int w, int value, int color) {
        graphics.fill(x, y, x + w, y + 6, SOFT);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 5, PAPER);
        graphics.fill(x + 1, y + 1, x + 1 + (w - 2) * Math.min(100, value) / 100, y + 5, color);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        rectangle(graphics, x, y, imageWidth, imageHeight, GOLD, DARK);
        rectangle(graphics, x + 7, y + 6, imageWidth - 14, 26, GOLD, GREEN);
        text(graphics, "AGRI EVOLUTION", x + 15, y + 15, PAPER);
        text(graphics, "BOTANICAL GENETICS LABORATORY", x + 168, y + 15, PAPER);

        for (int i = 0; i < MODES.length; i++) {
            int bx = x + 8 + i * 55;
            boolean locked = (i == 5 && menu.tier() < 2) || (i == 6 && menu.tier() < 3);
            rectangle(graphics, bx, y + 37, 52, 22, GOLD,
                    locked ? 0xFF66584A : menu.mode() == i ? PAPER : GREEN);
            text(graphics, MODES[i], bx + (52 - font.width(MODES[i])) / 2, y + 44,
                    locked ? 0xFFAE9B78 : menu.mode() == i ? INK : PAPER);
        }
        rectangle(graphics, x + 8, y + 64, 214, 70, GOLD, PAPER);
        rectangle(graphics, x + 226, y + 64, 162, 70, GOLD, PAPER);
        String[] names = {"SEED", "FEED", menu.mode() == 6 ? "SEED B" : "GENE", "BOOST", "RESULT"};
        int[] xslot = {25, 65, 105, 145, 185};
        for (int i = 0; i < names.length; i++) {
            rectangle(graphics, x + xslot[i] - 1, y + 82, 18, 18, SOFT, 0xFFDBCAAA);
            text(graphics, names[i], x + xslot[i] - 2, y + 71, INK);
        }
        text(graphics, "RESEARCH PROGRESS", x + 13, y + 106, GREEN);
        progress(graphics, x + 13, y + 120, 199,
                100 * menu.progress() / menu.requiredTicks(), GREEN);
        String mode = MODES[Math.max(0, Math.min(MODES.length - 1, menu.mode()))];
        text(graphics, "TRAIT PROFILE", x + 236, y + 70, GREEN);
        ItemStack seed = menu.inputSeed();
        CropGenome genes = GenomeStorage.fromSeed(seed);
        if (seed.getItem() instanceof SpeciesSeedItem species) {
            text(graphics, species.species().displayName(), x + 236, y + 83, INK);
            text(graphics, "G " + genes.growth() + "   Y " + genes.yield() + "   R " + genes.resilience(),
                    x + 236, y + 96, SOFT);
        } else text(graphics, "Insert a seed", x + 236, y + 93, SOFT);
        if (menu.mode() == 6) {
            ItemStack donor = menu.donorSeed();
            if (donor.getItem() instanceof SpeciesSeedItem other
                    && seed.getItem() instanceof SpeciesSeedItem first) {
                AdvancedHybridBreeder.HybridRecipe recipe =
                        AdvancedHybridBreeder.recipe(first.species(), other.species());
                String result = recipe == null ? "Parent species" : recipe.offspring().displayName();
                text(graphics, "HYBRID: " + font.plainSubstrByWidth(result, 98), x + 236, y + 109, GREEN);
                text(graphics, recipe == null ? "INHERIT: 50/50" : "CHANCE: "
                        + recipe.chance(com.agrievolution.content.CropSupportItem.labBonus(com.agrievolution.content.CropSupportItem.tier(menu.support())), false) + "% (+5 GH)",
                        x + 236, y + 121, SOFT);
            } else {
                text(graphics, "Add 2 different seeds", x + 236, y + 109, GREEN);
                text(graphics, "to mix genes", x + 236, y + 121, SOFT);
            }
        } else {
            text(graphics, explanation(menu.mode()), x + 236, y + 112, GREEN);
            RareTrait trait = RareTraitStorage.fromSeed(seed);
            text(graphics, "RARE: " + (trait == RareTrait.NONE ? "NONE" : trait.name()),
                    x + 236, y + 123, trait == RareTrait.NONE ? SOFT : 0xFF7750A5);
        }

        // Inventory has its own 171px-wide centre column; all notes remain strictly
        // outside that column. Previous text extended over clickable item slots.
        text(graphics, "INVENTORY", x + 13, y + 147, PAPER);
        rectangle(graphics, x + 90, y + 142, 171, 95, GOLD, 0xFFB79B72);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            rectangle(graphics, x + 95 + col * 18, y + 146 + row * 18, 18, 18, SOFT, 0xFFDED0B3);
        for (int col = 0; col < 9; col++)
            rectangle(graphics, x + 95 + col * 18, y + 216, 18, 18, SOFT, 0xFFDED0B3);
        text(graphics, "1 SEED", x + 13, y + 174, PAPER);
        text(graphics, "1 COMPOST", x + 13, y + 188, PAPER);
        text(graphics, "GRAFT: DNA", x + 13, y + 202, PAPER);
        text(graphics, "BOOST +", x + 13, y + 216, PAPER);
        text(graphics, "LAB TIER: " + menu.tier(), x + 270, y + 147, PAPER);
        if (menu.mode() == 6) {
            text(graphics, menu.tier() == 3 ? "HYBRID UNLOCKED" : "HYBRID LOCKED", x + 270, y + 162,
                    menu.tier() == 3 ? 0xFFA5E1B0 : 0xFFBBA58A);
            text(graphics, "2 DIFFERENT SEEDS", x + 270, y + 177, PAPER);
            text(graphics, "1 COMPOST", x + 270, y + 192, PAPER);
            text(graphics, "BONUS: SUPPORT", x + 270, y + 207, PAPER);
        } else {
            text(graphics, menu.tier() >= 2 ? "EVOLVE UNLOCKED" : "EVOLVE LOCKED", x + 270, y + 162,
                    menu.tier() >= 2 ? 0xFFA5E1B0 : 0xFFBBA58A);
            text(graphics, "EVOLVE: 20%", x + 270, y + 177, PAPER);
            text(graphics, "SUPPORT: +15%", x + 270, y + 192, PAPER);
            text(graphics, GeneticSampleItem.isValid(menu.sample()) ? "DNA READY" : "DNA REQUIRED", x + 270, y + 207, PAPER);
        }
        text(graphics, "MODE: " + mode, x + 270, y + 222, PAPER);
    }
    private String explanation(int mode) {
        return switch (mode) {
            case 0 -> "Improve growth";
            case 1 -> "Improve yield";
            case 2 -> "Improve resilience";
            case 3 -> "Extract donor genes";
            case 4 -> menu.inputSeed().getItem() instanceof SpeciesSeedItem && GeneticSampleItem.isValid(menu.sample())
                    && !menu.graftWouldImprove() ? "No gain: inputs kept" : "Transfer donor traits";
            case 5 -> "Rare trait research";
            default -> "Cross species";
        };
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= topPos + 37 && mouseY < topPos + 59) {
            for (int i = 0; i < MODES.length; i++) {
                int sx = leftPos + 8 + i * 55;
                if (mouseX >= sx && mouseX < sx + 52) {
                    if ((i == 5 && menu.tier() < 2) || (i == 6 && menu.tier() < 3)) return true;
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
