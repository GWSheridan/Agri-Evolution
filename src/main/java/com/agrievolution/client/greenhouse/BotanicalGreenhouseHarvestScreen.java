package com.agrievolution.client.greenhouse;

import com.agrievolution.client.GardenGuiTheme;

import com.agrievolution.content.BotanicalGreenhouseHarvestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated harvest settings; no inventory item slots overlap the controls. */
public final class BotanicalGreenhouseHarvestScreen extends AbstractContainerScreen<BotanicalGreenhouseHarvestMenu> {
    private static final int DARK = 0xFF281D17, GOLD = 0xFFC6A368,
            GREEN = 0xFF275841, PAPER = 0xFFF0DEB8, INK = 0xFF302518,
            MUTED = 0xFF947A57, RED = 0xFF9D463E;
    private static final String[] CATEGORIES = {"Resources", "Food / Flowers", "Genetic Seeds"};

    public BotanicalGreenhouseHarvestScreen(BotanicalGreenhouseHarvestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 314;
        imageHeight = 263;
    }
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        GardenGuiTheme.panel(g, x, y, w, h, color);
    }
    private void text(GuiGraphics g, String msg, int x, int y, int color) {
        g.drawString(font, msg, x, y, color, false);
    }
    private void button(GuiGraphics g, String label, int x, int y, int w, int color) {
        panel(g, x, y, w, 19, color);
        g.drawCenteredString(font, label, x + w / 2, y + 6, PAPER);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(g, x, y, imageWidth, imageHeight, DARK);
        panel(g, x + 8, y + 8, 298, 36, GREEN);
        text(g, "AGRI EVOLUTION  /  HARVEST CONTROL", x + 16, y + 16, PAPER);
        text(g, "Connect a chest on the selected side", x + 16, y + 30, PAPER);
        panel(g, x + 8, y + 49, 298, 197, PAPER);
        text(g, "OUTPUT CATEGORY", x + 16, y + 57, GREEN);
        text(g, "EXPORT", x + 151, y + 57, GREEN);
        text(g, "PRIORITY", x + 236, y + 57, GREEN);
        for (int c = 0; c < 3; c++) {
            int rowY = y + 71 + c * 25;
            text(g, CATEGORIES[c], x + 16, rowY + 6, INK);
            button(g, menu.categoryEnabled(c) ? "ON" : "OFF", x + 146, rowY, 64,
                    menu.categoryEnabled(c) ? GREEN : MUTED);
            button(g, switch (menu.categoryPriority(c)) {
                case 2 -> "HIGH"; case 1 -> "NORMAL"; default -> "LOW";
            }, x + 219, rowY, 77, GREEN);
        }
        button(g, "AUTO: " + (menu.exporting() ? "ON" : "OFF"), x + 16, y + 149, 132,
                menu.exporting() ? GREEN : MUTED);
        button(g, menu.nearFull() ? "WHEN 21/27" : "CONTINUOUS", x + 161, y + 149, 135, GREEN);
        button(g, "KEEP: " + menu.reserve(), x + 16, y + 176, 132, GREEN);
        button(g, "SIDE: " + menu.sideName(), x + 161, y + 176, 135, GREEN);
        text(g, "Moved: " + menu.transferred() + "   Stored slots: " + menu.slotsUsed() + "/27",
                x + 16, y + 201, INK);
        String state = !menu.exporting() ? "Export OFF - nothing moves"
                : !menu.connected() ? "Waiting for adjacent output inventory"
                : menu.rejected() ? "Output inventory refused items"
                : menu.nearFull() && menu.slotsUsed() < 21 ? "Waiting for 21 used storage slots"
                : "Export enabled - processing inventory";
        text(g, state, x + 16, y + 215, menu.rejected() ? RED : GREEN);
        button(g, "OPEN STORAGE", x + 182, y + 225, 114, GREEN);
        text(g, "No item deletion / exact genetics", x + 14, y + 248, PAPER);
    }
    @Override public boolean mouseClicked(double mx, double my, int mouseButton) {
        if (mouseButton == 0 && minecraft != null && minecraft.gameMode != null) {
            for (int c = 0; c < 3; c++) {
                int yy = topPos + 71 + c * 25;
                if (my >= yy && my < yy + 19) {
                    if (mx >= leftPos + 146 && mx < leftPos + 210) return click(10 + c);
                    if (mx >= leftPos + 219 && mx < leftPos + 296) return click(20 + c);
                }
            }
            if (my >= topPos + 149 && my < topPos + 168) {
                if (mx >= leftPos + 16 && mx < leftPos + 148) return click(0);
                if (mx >= leftPos + 161 && mx < leftPos + 296) return click(1);
            }
            if (my >= topPos + 176 && my < topPos + 195) {
                if (mx >= leftPos + 16 && mx < leftPos + 148) return click(2);
                if (mx >= leftPos + 161 && mx < leftPos + 296) return click(3);
            }
            if (my >= topPos + 225 && my < topPos + 244 && mx >= leftPos + 182 && mx < leftPos + 296)
                return click(4);
        }
        return super.mouseClicked(mx, my, mouseButton);
    }
    private boolean click(int code) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, code);
        return true;
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        renderTooltip(g, mx, my);
    }
}
