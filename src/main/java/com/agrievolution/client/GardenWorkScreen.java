package com.agrievolution.client;

import com.agrievolution.content.GardenWork;
import com.agrievolution.content.GardenWorkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GardenWorkScreen extends AbstractContainerScreen<GardenWorkMenu> {
    private static final int DARK = 0xFF302419, PAPER = 0xFFF1DFB8, INK = 0xFF342519;
    private static final int GREEN = 0xFF285947, MUTED = 0xFF806849;
    private static final int ROW_Y = 36, ROW_PITCH = 41, ITEM_X = 20, ITEM_Y = 18, ITEM_PITCH = 21;
    private static final int REWARD_X = 204, ACTION_X = 232, ACTION_Y = 11, ACTION_WIDTH = 62;
    private static final String[] TIERS = {"Early", "Mid", "Late"};
    private static final String[] TABS = {"Early", "Mid", "Late", "Commissions"};
    private static final int TAB_X = 14, TAB_Y = 146, TAB_WIDTH = 69, TAB_PITCH = 72;

    public GardenWorkScreen(GardenWorkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 312;
        imageHeight = 254;
    }

    @Override protected void renderBg(GuiGraphics gui, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(gui, x, y, imageWidth, imageHeight, DARK);
        GardenGuiTheme.panel(gui, x + 8, y + 7, imageWidth - 16, 24, GREEN);
        gui.drawCenteredString(font, title, x + imageWidth / 2, y + 15, PAPER);
        for (int row = 0; row < GardenWork.ROWS; row++) drawJob(gui, row);
        if (!menu.preserving()) for (int page = 0; page < TABS.length; page++) {
            int tab = x + TAB_X + TAB_PITCH * page;
            GardenGuiTheme.panel(gui, tab, y + TAB_Y, TAB_WIDTH, 13, page == menu.page() ? GREEN : MUTED);
            gui.drawCenteredString(font, TABS[page], tab + TAB_WIDTH / 2, y + TAB_Y + 3, PAPER);
        }
        GardenGuiTheme.panel(gui, x + 8, y + 163, imageWidth - 16, 85, PAPER);
        gui.drawString(font, "Inventory", x + 17, y + 172, INK, false);
        for (var slot : menu.slots) GardenGuiTheme.slot(gui, x + slot.x, y + slot.y);
    }

    private void drawJob(GuiGraphics gui, int row) {
        int x = leftPos, y = rowTop(row);
        GardenGuiTheme.panel(gui, x + 8, y, imageWidth - 16, menu.preserving() ? 38 : 35, PAPER);
        var job = menu.job(row);
        if (!menu.unlocked(row)) {
            int previous = menu.tier(row) - 1;
            gui.drawString(font, menu.commissions() ? job.title() + " locked" : TIERS[menu.tier(row)] + " orders locked", x + ITEM_X, y + 8, INK, false);
            gui.drawString(font, "Complete 2 " + TIERS[previous].toLowerCase(java.util.Locale.ROOT) + " orders (" + menu.completed(previous) + "/2)",
                    x + ITEM_X, y + 22, MUTED, false);
            return;
        }
        gui.drawString(font, job.title(), x + ITEM_X, y + 5, INK, false);
        for (int i = 0; i < job.ingredients().size(); i++) {
            drawItem(gui, job.ingredients().get(i), x + ITEM_X + i * ITEM_PITCH, y + ITEM_Y);
        }
        gui.drawString(font, menu.preserving() ? "Makes" : "Reward", x + 165, y + 22, INK, false);
        drawItem(gui, job.reward(), x + REWARD_X, y + ITEM_Y);
        boolean ready = menu.ready(row);
        GardenGuiTheme.panel(gui, x + ACTION_X, y + ACTION_Y, ACTION_WIDTH, 18, ready ? GREEN : MUTED);
        gui.drawCenteredString(font, menu.preserving() ? "Preserve" : menu.claimed(row) ? "Claimed" : "Deliver",
                x + ACTION_X + ACTION_WIDTH / 2, y + ACTION_Y + 5, PAPER);
    }

    private void drawItem(GuiGraphics gui, GardenWork.Ingredient ingredient, int x, int y) {
        var stack = ingredient.stack();
        GardenGuiTheme.slot(gui, x, y);
        gui.renderItem(stack, x, y);
        gui.renderItemDecorations(font, stack, x, y);
    }

    private int rowTop(int row) { return topPos + ROW_Y + row * (menu.preserving() ? ROW_PITCH : 36); }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= y && mouseY < y + height;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !menu.preserving()) for (int page = 0; page < TABS.length; page++) {
            if (!inside(mouseX, mouseY, TAB_X + TAB_PITCH * page, topPos + TAB_Y, TAB_WIDTH, 13)) continue;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.pageAction(page));
            }
            return true;
        }
        if (button == 0) for (int row = 0; row < GardenWork.ROWS; row++) {
            if (!menu.unlocked(row) || !inside(mouseX, mouseY, ACTION_X, rowTop(row) + ACTION_Y, ACTION_WIDTH, 18)) continue;
            if (menu.ready(row) && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.action(row));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
        for (int row = 0; row < GardenWork.ROWS; row++) {
            if (!menu.unlocked(row)) continue;
            var job = menu.job(row);
            int y = rowTop(row) + ITEM_Y;
            for (int i = 0; i < job.ingredients().size(); i++) {
                if (inside(mouseX, mouseY, ITEM_X + i * ITEM_PITCH, y, 16, 16)) {
                    gui.renderTooltip(font, job.ingredients().get(i).stack(), mouseX, mouseY);
                    return;
                }
            }
            if (inside(mouseX, mouseY, REWARD_X, y, 16, 16)) {
                gui.renderTooltip(font, job.reward().stack(), mouseX, mouseY);
                return;
            }
            if (!menu.ready(row) && !menu.claimed(row) && inside(mouseX, mouseY, ACTION_X, rowTop(row) + ACTION_Y, ACTION_WIDTH, 18)) {
                gui.renderTooltip(font, Component.literal("Bring the listed items in your inventory"), mouseX, mouseY);
                return;
            }
        }
    }
}
