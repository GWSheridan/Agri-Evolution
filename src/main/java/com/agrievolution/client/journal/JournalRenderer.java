package com.agrievolution.client.journal;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.content.FlowerHarvests;

import com.agrievolution.genetics.CropSpecies;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Reusable pixel-art drawing primitives. No game state is stored here. */
public final class JournalRenderer {
    private JournalRenderer() {}

    static String flowerHarvestName(CropSpecies species) {
        return new ItemStack(FlowerHarvests.dye(species)).getHoverName().getString();
    }

    public static void frame(GuiGraphics gui, Font font, JournalNavigation nav,
                             JournalSnapshot snapshot, int mx, int my) {
        sprite(gui, "journal_background", 0, 0, 430, 270);
        gui.drawString(font, "AGRI EVOLUTION", 37, 23, JournalTheme.GOLD, false);
        gui.drawString(font, nav.building()?"BUILDING & MACHINES":"GRANDMAS JOURNAL", 209, 23, 0xE8D4A3, false);
        JournalNavigation.Section[] tabs = nav.sections();
        for (int i = 0; i < tabs.length; i++) {
            int x = tabX(i);
            int tabWidth=nav.building()?62:58;
            boolean selected = nav.section() == tabs[i];
            boolean hovering = inside(mx, my, x, 50, tabWidth, 16);
            sprite(gui, selected ? "tab_active" : "tab_inactive", x, 50, tabWidth, 16);
            String label = nav.sectionTitle(tabs[i]);
            var tabText = JournalFonts.body(label);
            gui.drawString(font, tabText, x + tabWidth/2 - font.width(tabText) / 2, 54,
                    selected ? JournalTheme.INK : (hovering ? 0xFFF0D0 : 0xDBC29B), false);
        }
        gui.drawString(font, JournalFonts.body(nav.building()?"BUILDING & MACHINE GUIDE":"DISCOVERED " + snapshot.discoveredCount() + "/" + snapshot.speciesCount()),
                33, 250, JournalTheme.GREEN, false);
        gui.drawString(font, JournalFonts.body(nav.building()?"R: recipes  B: builds":"Agri Evolution  /  Field Research"), 226, 250, JournalTheme.MUTED, false);
    }

    public static int tabX(int index) { return 24 + index * 65; }

    public static void sprite(GuiGraphics gui, String sprite, int x, int y, int width, int height) {
        if (sprite.startsWith("tab_active") || sprite.startsWith("tab_inactive")) {
            GardenGuiTheme.panel(gui,x,y,width,height,
                    sprite.startsWith("tab_active") ? 0xFFF0DEB8 : 0xFF275841);
            return;
        }
        gui.blit(JournalTheme.texture(sprite), x, y, 0, 0, width, height, width, height);
        if (sprite.startsWith("journal_background")) GardenGuiTheme.binding(gui,x,y,width,height);
    }

    public static void speciesIcon(GuiGraphics gui, CropSpecies species, boolean discovered,
                                   int x, int y, int multiplier) {
        if (discovered) {
            currentCrop(gui, species, x, y, 32 * multiplier);
            return;
        }
        String file = discovered ? "species/" + species.id() : "species/undiscovered";
        ResourceLocation sprite = JournalTheme.texture(file);
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(multiplier, multiplier, 1.0f);
        gui.blit(sprite, 0, 0, 0, 0, 32, 32, 32, 32);
        gui.pose().popPose();
    }

    /** Read the approved in-game mature crop directly; no stale journal artwork. */
    public static void currentCrop(GuiGraphics gui, CropSpecies species, int x, int y, int size) {
        String name = species == CropSpecies.WILD ? "agri" : species.id();
        var texture = ResourceLocation.fromNamespaceAndPath("agri_evolution", "textures/block/" + name + "_crop_stage7.png");
        gui.blit(texture, x, y, size, size, 0, 0, 64, 64, 64, 64);
    }

    public static void heading(GuiGraphics gui, Font font, String heading, int x, int y, int colour) {
        gui.drawString(font, JournalFonts.body(heading), x, y, colour, false);
        gui.fill(x, y + 13, x + 158, y + 14, 0xFFC8AA72);
    }

    public static void progress(GuiGraphics gui, int x, int y, int width, int value, int max, int colour) {
        gui.fill(x, y, x + width, y + 7, 0xFF67553A);
        gui.fill(x + 1, y + 1, x + width - 1, y + 6, 0xFFE1D2AD);
        int length = (width - 2) * Math.max(0, Math.min(value, max)) / Math.max(1, max);
        if (length > 0) gui.fill(x + 1, y + 1, x + 1 + length, y + 6, 0xFF000000 | colour);
    }

    public static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && y >= top && x < left + w && y < top + h;
    }
}


