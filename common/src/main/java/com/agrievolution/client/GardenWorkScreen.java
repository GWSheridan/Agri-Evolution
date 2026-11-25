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
    private static final int CONTRIBUTE_X = 162, CONTRIBUTE_WIDTH = 69, CLAIM_X = 236, CLAIM_WIDTH = 60;
    private static final int SHARED_ACTION_Y = 5, SHARED_ACTION_HEIGHT = 15;
    private static final String[] TIERS = {"Early", "Mid", "Late"};
    private static final String[] TABS = {"Early", "Mid", "Late", "Commissions", "Seeds", "Shared", "Custom"};
    private static final int[] TAB_X = {11, 46, 77, 111, 186, 221, 262};
    private static final int[] TAB_WIDTH = {32, 28, 31, 72, 32, 38, 39};
    private static final int TAB_Y = 146;
    private final int[] shownAction = new int[GardenWork.ROWS];
    private final boolean[] shownReady = new boolean[GardenWork.ROWS];
    private final boolean[] shownClaimReady = new boolean[GardenWork.ROWS];
    private boolean shownShared;

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
        shownShared = menu.shared();
        for (int row = 0; row < GardenWork.ROWS; row++) drawJob(gui, row);
        if (!menu.preserving()) for (int page = 0; page < tabCount(); page++) {
            int tab = x + TAB_X[page], width = TAB_WIDTH[page];
            int current = Math.min(menu.page(), GardenWorkMenu.CUSTOM_PAGE);
            GardenGuiTheme.panel(gui, tab, y + TAB_Y, width, 13, page == current ? GREEN : MUTED);
            gui.drawCenteredString(font, TABS[page], tab + width / 2, y + TAB_Y + 3, PAPER);
        }
        if (menu.custom() && menu.customPages() > 1) {
            pageArrow(gui, 16, "<", menu.page() > GardenWorkMenu.CUSTOM_PAGE);
            pageArrow(gui, imageWidth - 34, ">", menu.page() < GardenWorkMenu.CUSTOM_PAGE + menu.customPages() - 1);
        }
        GardenGuiTheme.panel(gui, x + 8, y + 163, imageWidth - 16, 85, PAPER);
        gui.drawString(font, "Inventory", x + 17, y + 172, INK, false);
        for (var slot : menu.slots) GardenGuiTheme.slot(gui, x + slot.x, y + slot.y);
    }

    private void drawJob(GuiGraphics gui, int row) {
        shownReady[row] = shownClaimReady[row] = false;
        var job = menu.job(row);
        if (job == null) return;
        int x = leftPos, y = rowTop(row);
        GardenGuiTheme.panel(gui, x + 8, y, imageWidth - 16, menu.preserving() ? 38 : 35, PAPER);
        if (!menu.unlocked(row)) {
            int previous = menu.tier(row) - 1;
            gui.drawString(font, menu.commissions() ? job.title() + " locked" : TIERS[menu.tier(row)] + " orders locked", x + ITEM_X, y + 8, INK, false);
            gui.drawString(font, "Complete 2 " + TIERS[previous].toLowerCase(java.util.Locale.ROOT) + " orders (" + menu.completed(previous) + "/2)",
                    x + ITEM_X, y + 22, MUTED, false);
            return;
        }
        int titleEnd = shownShared ? CONTRIBUTE_X : ACTION_X;
        gui.drawString(font, font.plainSubstrByWidth(job.title(), titleEnd - ITEM_X - 8), x + ITEM_X, y + 5, INK, false);
        for (int i = 0; i < job.ingredients().size(); i++) {
            if (menu.shared()) {
                int remaining = menu.remaining(row, i);
                var stack = job.ingredients().get(i).stack().copyWithCount(Math.max(1, remaining));
                drawStack(gui, stack, x + ITEM_X + i * ITEM_PITCH, y + ITEM_Y);
                if (remaining == 0) gui.drawString(font, "✓", x + ITEM_X + i * ITEM_PITCH + 8, y + ITEM_Y + 8, GREEN, false);
            } else drawItem(gui, job.ingredients().get(i), x + ITEM_X + i * ITEM_PITCH, y + ITEM_Y);
        }
        if (menu.seedCommissions()) {
            var rules = job.ingredients().getFirst().requirements(true);
            gui.drawString(font, font.plainSubstrByWidth(rules, 120), x + ITEM_X + ITEM_PITCH, y + 22, INK, false);
        }
        if (shownShared) {
            drawSharedControls(gui, row, x, y);
            return;
        }
        gui.drawString(font, menu.preserving() ? "Makes" : "Reward", x + 165, y + 22, INK, false);
        drawItem(gui, job.reward(), x + REWARD_X, y + ITEM_Y);
        boolean ready = menu.ready(row);
        shownReady[row] = ready;
        shownAction[row] = menu.action(row);
        GardenGuiTheme.panel(gui, x + ACTION_X, y + ACTION_Y, ACTION_WIDTH, 18, ready ? GREEN : MUTED);
        gui.drawCenteredString(font, menu.preserving() ? "Preserve" : menu.claimed(row) ? "Claimed" : "Deliver",
                x + ACTION_X + ACTION_WIDTH / 2, y + ACTION_Y + 5, PAPER);
    }

    private void drawSharedControls(GuiGraphics gui, int row, int x, int y) {
        shownAction[row] = menu.contributeAction(row);
        shownReady[row] = menu.canContribute(row);
        shownClaimReady[row] = menu.canClaim(row);
        sharedButton(gui, x + CONTRIBUTE_X, y, CONTRIBUTE_WIDTH, "Contribute", shownReady[row]);
        sharedButton(gui, x + CLAIM_X, y, CLAIM_WIDTH, "Claim", shownClaimReady[row]);
        int contributed = menu.contributed(row);
        gui.drawString(font, contributed > 0 ? "You gave " + contributed : "Still needed", x + 105, y + 23, INK, false);
        if (menu.claimable(row) > 0) {
            gui.drawString(font, "Emeralds ×" + menu.claimable(row), x + 204, y + 23, INK, false);
        }
    }

    private void sharedButton(GuiGraphics gui, int x, int y, int width, String label, boolean ready) {
        GardenGuiTheme.panel(gui, x, y + SHARED_ACTION_Y, width, SHARED_ACTION_HEIGHT, ready ? GREEN : MUTED);
        GardenGuiTheme.center(gui, label, x + width / 2, y + SHARED_ACTION_Y + 4, PAPER);
    }

    private int tabCount() { return menu.customPages() == 0 ? TABS.length - 1 : TABS.length; }

    private void pageArrow(GuiGraphics gui, int x, String label, boolean enabled) {
        GardenGuiTheme.panel(gui, leftPos + x, topPos + 11, 18, 15, enabled ? GREEN : MUTED);
        gui.drawCenteredString(font, label, leftPos + x + 9, topPos + 15, PAPER);
    }

    private void drawItem(GuiGraphics gui, GardenWork.Ingredient ingredient, int x, int y) {
        drawStack(gui, ingredient.stack(), x, y);
    }

    private void drawStack(GuiGraphics gui, net.minecraft.world.item.ItemStack stack, int x, int y) {
        GardenGuiTheme.slot(gui, x, y);
        gui.renderItem(stack, x, y);
        gui.renderItemDecorations(font, stack, x, y);
    }

    private int rowTop(int row) { return topPos + ROW_Y + row * (menu.preserving() ? ROW_PITCH : 36); }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= y && mouseY < y + height;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.custom() && menu.customPages() > 1) {
            if (inside(mouseX, mouseY, 16, topPos + 11, 18, 15) && menu.page() > GardenWorkMenu.CUSTOM_PAGE) {
                switchPage(menu.page() - 1);
                return true;
            }
            if (inside(mouseX, mouseY, imageWidth - 34, topPos + 11, 18, 15)
                    && menu.page() < GardenWorkMenu.CUSTOM_PAGE + menu.customPages() - 1) {
                switchPage(menu.page() + 1);
                return true;
            }
        }
        if (button == 0 && !menu.preserving()) for (int page = 0; page < tabCount(); page++) {
            if (!inside(mouseX, mouseY, TAB_X[page], topPos + TAB_Y, TAB_WIDTH[page], 13)) continue;
            switchPage(page);
            return true;
        }
        if (button == 0) for (int row = 0; row < GardenWork.ROWS; row++) {
            if (shownShared) {
                int y = rowTop(row) + SHARED_ACTION_Y;
                if (inside(mouseX, mouseY, CONTRIBUTE_X, y, CONTRIBUTE_WIDTH, SHARED_ACTION_HEIGHT)) {
                    if (shownReady[row]) send(shownAction[row]);
                    return true;
                }
                if (inside(mouseX, mouseY, CLAIM_X, y, CLAIM_WIDTH, SHARED_ACTION_HEIGHT)) {
                    if (shownClaimReady[row]) send(menu.claimAction(row));
                    return true;
                }
            } else if (inside(mouseX, mouseY, ACTION_X, rowTop(row) + ACTION_Y, ACTION_WIDTH, 18)) {
                if (shownReady[row]) send(shownAction[row]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchPage(int page) {
        send(menu.pageAction(page));
    }

    private void send(int action) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }
    }

    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
        for (int row = 0; row < GardenWork.ROWS; row++) {
            if (!menu.unlocked(row)) continue;
            var job = menu.job(row);
            if (job == null) continue;
            int y = rowTop(row) + ITEM_Y;
            for (int i = 0; i < job.ingredients().size(); i++) {
                if (inside(mouseX, mouseY, ITEM_X + i * ITEM_PITCH, y, 16, 16)) {
                    var ingredient = job.ingredients().get(i);
                    if (ingredient.qualified()) {
                        var hint = Component.literal(ingredient.count() + " × " + ingredient.stack().getHoverName().getString()
                                + "\n" + ingredient.requirements(false));
                        gui.renderTooltip(font, font.split(hint, 175), mouseX, mouseY);
                    } else if (menu.shared()) gui.renderTooltip(font, Component.literal(ingredient.stack().getHoverName().getString()
                            + " · " + menu.remaining(row, i) + " still needed"), mouseX, mouseY);
                    else gui.renderTooltip(font, ingredient.stack(), mouseX, mouseY);
                    return;
                }
            }
            if (!menu.shared() && inside(mouseX, mouseY, REWARD_X, y, 16, 16)) {
                gui.renderTooltip(font, job.reward().stack(), mouseX, mouseY);
                return;
            }
            if (shownShared) {
                int actionY = rowTop(row) + SHARED_ACTION_Y;
                if (inside(mouseX, mouseY, CLAIM_X, actionY, CLAIM_WIDTH, SHARED_ACTION_HEIGHT) && !shownClaimReady[row]) {
                    String message = menu.claimable(row) > 0 ? "Make room for emeralds in your inventory" : "Rewards are ready when the order is filled";
                    gui.renderTooltip(font, Component.literal(message), mouseX, mouseY);
                    return;
                }
                if (inside(mouseX, mouseY, CONTRIBUTE_X, actionY, CONTRIBUTE_WIDTH, SHARED_ACTION_HEIGHT) && !shownReady[row]) {
                    gui.renderTooltip(font, Component.literal("Bring needed items in your main inventory"), mouseX, mouseY);
                    return;
                }
            } else if (!shownReady[row] && !menu.claimed(row) && inside(mouseX, mouseY, ACTION_X, rowTop(row) + ACTION_Y, ACTION_WIDTH, 18)) {
                gui.renderTooltip(font, Component.literal("Bring the listed items in your inventory"), mouseX, mouseY);
                return;
            }
        }
    }
}
