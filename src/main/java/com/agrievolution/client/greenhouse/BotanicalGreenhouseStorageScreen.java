package com.agrievolution.client.greenhouse;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.BotanicalGreenhouseStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Crisp native-resolution storage screen; slots align with the server menu. */
public final class BotanicalGreenhouseStorageScreen extends com.agrievolution.client.ScrollableGardenScreen<BotanicalGreenhouseStorageMenu> {
    private static final int DARK = 0xFF281D17, GOLD = 0xFFC6A368;
    private static final int GREEN = 0xFF275841, PAPER = 0xFFF0DEB8;
    public BotanicalGreenhouseStorageScreen(BotanicalGreenhouseStorageMenu menu, Inventory inv, Component name) {
        super(menu, inv, name);
        imageWidth = 196;
        imageHeight = 207;
    }
    @Override protected void renderBg(GuiGraphics g, float delta, int mx, int my) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(g,x,y,imageWidth,imageHeight,DARK);
        GardenGuiTheme.panel(g,x+9,y+9,imageWidth-18,30,GREEN);
        GardenGuiTheme.panel(g,x+9,y+43,imageWidth-18,60,PAPER);
        GardenGuiTheme.panel(g,x+9,y+105,imageWidth-18,96,PAPER);
        for (int i = 0; i < 27; i++) slotBg(g, x + 17 + i % 9 * 18, y + 47 + i / 9 * 18);
        for (int i = 0; i < 27; i++) slotBg(g, x + 17 + i % 9 * 18, y + 118 + i / 9 * 18);
        for (int i = 0; i < 9; i++) slotBg(g, x + 17 + i * 18, y + 182);
        g.drawString(font, "AUTOMATED STORAGE", x + 18, y + 15, PAPER, false);
        g.drawString(font, "Harvested crops & genetics", x + 18, y + 28, PAPER, false);
        g.drawString(font, "Inventory", x + 17, y + 108, 0xFF302518, false);
    }
    private static void slotBg(GuiGraphics g, int x, int y) {
        GardenGuiTheme.slot(g,x,y);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }
}
