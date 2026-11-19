package com.agrievolution.client;

import com.agrievolution.content.IrrigationControllerBlockEntity;
import com.agrievolution.content.IrrigationControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IrrigationControllerScreen extends com.agrievolution.client.ScrollableGardenScreen<IrrigationControllerMenu> {
    private static final int GREEN = 0xff164534;
    private static final int PAPER = 0xfff0dfbb;
    private static final int INK = 0xff352716;
    private static final int ZONES = IrrigationControllerBlockEntity.ZONES;
    private static final int FIRST_ROW = 72, ROW_PITCH = 21;
    private final Button[][] controls = new Button[ZONES][5];
    private Button reserve, mode;

    public IrrigationControllerScreen(IrrigationControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 326;
        imageHeight = 224;
    }

    @Override protected void init() {
        super.init();
        mode = button(12, 29, 109, 0, 5);
        reserve = button(127, 29, 124, 0, 4);
        for (int row = 0; row < ZONES; row++) {
            int y = FIRST_ROW + row * ROW_PITCH;
            controls[row][0] = button(112, y, 18, row, 6);
            controls[row][1] = button(172, y, 18, row, 0);
            controls[row][2] = button(194, y, 42, row, 1);
            controls[row][3] = button(240, y, 40, row, 2);
            controls[row][4] = button(284, y, 30, row, 3);
            controls[row][0].setMessage(Component.literal("-"));
            controls[row][1].setMessage(Component.literal("+"));
            controls[row][4].setMessage(Component.literal("Unlink"));
        }
    }

    private Button button(int x, int y, int width, int row, int action) {
        return addRenderableWidget(GardenGuiTheme.button(Component.empty(), leftPos + x, topPos + y, width, 18, ignored -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, row + action * ZONES);
            }
        }));
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        GardenGuiTheme.panel(g, leftPos, topPos, imageWidth, imageHeight, 0xff38291e);
        GardenGuiTheme.panel(g, leftPos + 7, topPos + 7, 312, 18, GREEN);
        GardenGuiTheme.panel(g, leftPos + 7, topPos + 53, 312, 150, PAPER);
        g.drawString(font, title, leftPos + 13, topPos + 12, PAPER, false);
        g.drawString(font, "Zone", leftPos + 14, topPos + 59, GREEN, false);
        GardenGuiTheme.center(g, "Start time", leftPos + 151, topPos + 59, GREEN);
        GardenGuiTheme.center(g, "Length", leftPos + 215, topPos + 59, GREEN);
        mode.setMessage(Component.literal(menu.enabled() ? "Scheduling on" : "Scheduling off"));
        reserve.setMessage(Component.literal("Reserve " + menu.reserve() + " mB"));
        for (int row = 0; row < ZONES; row++) {
            int y = topPos + FIRST_ROW + row * ROW_PITCH;
            boolean linked = menu.linked(row);
            for (var control : controls[row]) control.visible = linked;
            if (linked) {
                int hour = (menu.hour(row) + 6) % 24;
                GardenGuiTheme.center(g, String.format(java.util.Locale.ROOT, "%02d:00", hour), leftPos + 150, y + 5, INK);
                controls[row][2].setMessage(Component.literal(menu.duration(row) / 20 + "s"));
                controls[row][3].setMessage(Component.literal(menu.manual() == row ? "Stop" : "Run"));
            }
            String label = (menu.active() == row ? "▶ " : "") + menu.label(row);
            g.drawString(font, font.plainSubstrByWidth(label, 95), leftPos + 14, y + 5, INK, false);
        }
        g.drawString(font, "Link named valves with a Spanner.", leftPos + 12, topPos + 209, PAPER, false);
    }

    @Override protected void renderLabels(GuiGraphics g, int x, int y) {}

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        int row = Math.floorDiv(mouseY - topPos - FIRST_ROW, ROW_PITCH);
        if (row >= 0 && row < ZONES && mouseX >= leftPos + 14 && mouseX < leftPos + 109
                && mouseY < topPos + FIRST_ROW + row * ROW_PITCH + 18) {
            gui.renderTooltip(font, font.split(Component.literal(menu.label(row)), Math.min(240, width - 16)), mouseX, mouseY);
        }
    }
}
