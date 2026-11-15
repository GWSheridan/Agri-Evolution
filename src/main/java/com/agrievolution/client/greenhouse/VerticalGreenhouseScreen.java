package com.agrievolution.client.greenhouse;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.content.VerticalGreenhouseBlockEntity;
import com.agrievolution.content.VerticalGreenhouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class VerticalGreenhouseScreen extends AbstractContainerScreen<VerticalGreenhouseMenu> {
    private static final int DARK = 0xFF302419, PAPER = 0xFFF1DFB8, INK = 0xFF342519;
    private static final int GREEN = 0xFF285947, MUTED = 0xFF806849, BLUE = 0xFF367F96;
    private static final int CONTROL_Y = 36, CONTROL_HEIGHT = 19, CONTROL_WIDTH = 54;
    private static final int GROW_X = 137, BREED_X = 195, PAIR_X = 253;
    private static final int PARENT_Y = 73, PARENT_WIDTH = 14, PARENT_HEIGHT = 14, PARENT_PITCH = 16;
    private static final int INTAKE_X = 17, INTAKE_Y = 117, INTAKE_WIDTH = 103, INTAKE_HEIGHT = 13;
    private static final int ROUTE_X = 16, ROUTE_Y = 188, ROUTE_PITCH = 13, ROUTE_WIDTH = 62;
    private static final int BATCH_X = 247, BATCH_Y = 188, BATCH_WIDTH = 62, RESET_Y = 203;

    public VerticalGreenhouseScreen(VerticalGreenhouseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 322;
        imageHeight = 265;
    }

    @Override protected void renderBg(GuiGraphics gui, float delta, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        GardenGuiTheme.panel(gui, x, y, imageWidth, imageHeight, DARK);
        GardenGuiTheme.panel(gui, x + 8, y + 7, 306, 23, GREEN);
        gui.drawCenteredString(font, title, x + imageWidth / 2, y + 14, PAPER);
        GardenGuiTheme.panel(gui, x + 8, y + 33, 306, 136, PAPER);
        GardenGuiTheme.panel(gui, x + 8, y + 173, 306, 84, PAPER);

        gui.drawString(font, menu.water() + " / " + VerticalGreenhouseBlockEntity.CAPACITY + " mB", x + 17, y + 39, INK, false);
        bar(gui, x + 17, y + 51, 111, menu.water(), VerticalGreenhouseBlockEntity.CAPACITY, BLUE);
        control(gui, menu.growEnabled() ? "Grow ON" : "Grow OFF", menu.growEnabled(), GROW_X);
        control(gui, menu.breedEnabled() ? "Breed ON" : "Breed OFF", menu.breedEnabled(), BREED_X);
        control(gui, menu.fixedPair() ? "Fixed pair" : "Auto pair", true, PAIR_X);
        gui.drawString(font, "Growth: " + status(menu.growStatus()), x + 17, y + 61, INK, false);
        String breeding = "Breeding: " + status(menu.breedStatus());
        if (menu.fixedPair() && menu.breedProgress() > 0) breeding += " " + menu.breedProgress() + "%";
        gui.drawString(font, breeding, x + 168, y + 61, INK, false);
        if (!menu.fixedPair()) {
            gui.drawString(font, "Seed trays", x + 17, y + 76, GREEN, false);
            if (menu.parentA() > 0 && menu.parentB() > 0) {
                String pair = "Trays " + menu.parentA() + " + " + menu.parentB() + "   " + menu.breedProgress() + "%";
                gui.drawString(font, pair, x + 194, y + 76, GREEN, false);
            }
        }

        for (var slot : menu.slots) GardenGuiTheme.slot(gui, x + slot.x, y + slot.y);
        for (int tray = 0; tray < VerticalGreenhouseBlockEntity.TRAYS; tray++) {
            int trayX = x + VerticalGreenhouseMenu.FIRST_TRAY_X + tray * VerticalGreenhouseMenu.TRAY_PITCH;
            if (menu.fixedPair()) {
                parentButton(gui, "A", parentX(tray), menu.selectedA() == tray + 1);
                parentButton(gui, "B", parentX(tray) + PARENT_PITCH, menu.selectedB() == tray + 1);
            }
            int progress = menu.trayProgress(tray);
            bar(gui, trayX - 7, y + 109, 30, progress, 100, GREEN);
            if (progress < 0) gui.drawCenteredString(font, "-", trayX + 8, y + 105, MUTED);
        }
        drawWaterIntake(gui);
        gui.drawString(font, "Harvest", x + 17, y + VerticalGreenhouseMenu.HARVEST_Y + 4, INK, false);
        gui.drawString(font, "Offspring", x + 17, y + VerticalGreenhouseMenu.OFFSPRING_Y + 4, INK, false);
        gui.drawString(font, "Seed batch", x + BATCH_X, y + 177, INK, false);
        String batch = menu.batchTarget() == 0 ? "Unlimited" : menu.batchCount() + " / " + menu.batchTarget();
        smallButton(gui, batch, BATCH_X, BATCH_Y, BATCH_WIDTH, GREEN);
        if (menu.batchTarget() > 0) smallButton(gui, "Reset batch", BATCH_X, RESET_Y, BATCH_WIDTH, MUTED);
        gui.drawString(font, "Outputs", x + ROUTE_X, y + 177, INK, false);
        for (int face = 0; face < VerticalGreenhouseBlockEntity.ROUTE_COUNT; face++) {
            var direction = VerticalGreenhouseBlockEntity.outputFace(face);
            var route = menu.outputRoute(face);
            String label = (face == 0 ? "Down" : direction.getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT)) + ": " + switch (route) {
                case OFF -> "Off";
                case PRODUCE -> "Crops";
                case SEEDS -> "Seeds";
                case BOTH -> "Both";
            };
            smallButton(gui, label, ROUTE_X, ROUTE_Y + face * ROUTE_PITCH, ROUTE_WIDTH, route == VerticalGreenhouseBlockEntity.OutputRoute.OFF ? MUTED : GREEN);
        }
    }

    private void control(GuiGraphics gui, String label, boolean enabled, int offset) {
        int x = leftPos + offset;
        GardenGuiTheme.panel(gui, x, topPos + CONTROL_Y, CONTROL_WIDTH, CONTROL_HEIGHT, enabled ? GREEN : MUTED);
        gui.drawCenteredString(font, label, x + CONTROL_WIDTH / 2, topPos + CONTROL_Y + 6, PAPER);
    }

    private void smallButton(GuiGraphics gui, String label, int x, int y, int width, int color) {
        GardenGuiTheme.panel(gui, leftPos + x, topPos + y, width, 12, color);
        gui.drawCenteredString(font, label, leftPos + x + width / 2, topPos + y + 2, PAPER);
    }

    private void drawWaterIntake(GuiGraphics gui) {
        int x = leftPos + INTAKE_X;
        int y = topPos + INTAKE_Y;
        var intake = menu.waterIntake();
        String label = switch (intake) {
            case DEMAND -> "Pipes: Demand";
            case KEEP_FULL -> "Pipes: Keep full";
            case OFF -> "Pipes: Off";
        };
        GardenGuiTheme.panel(gui, x, y, INTAKE_WIDTH, INTAKE_HEIGHT,
                intake == VerticalGreenhouseBlockEntity.WaterIntake.OFF ? MUTED : GREEN);
        gui.drawCenteredString(font, label, x + INTAKE_WIDTH / 2, y + 3, PAPER);
        String status = switch (menu.waterStatus()) {
            case CHECKING -> "Checking intake";
            case OFF -> "Automatic intake off";
            case WAITING -> "Waiting for a job";
            case READY -> intake == VerticalGreenhouseBlockEntity.WaterIntake.KEEP_FULL ? "Tank topped up" : "Water ready";
            case REFILLING -> "Drawing from supply";
            case NO_SOURCE -> "No water source reached";
            case EMPTY_SOURCE -> "Supply needs water";
            case BLOCKED_VALVE -> "Valve blocking supply";
            case NETWORK_LIMIT -> "Pipe network too large";
            case INCOMPLETE -> "Column incomplete";
            case BLOCKED_PORT -> "Pipe port disabled";
            case UNLOADED_SUPPLY -> "Supply chunk unloaded";
            case INCOMPLETE_RESERVOIR -> "Reservoir incomplete";
        };
        gui.drawString(font, status, leftPos + VerticalGreenhouseMenu.FIRST_OUTPUT_X, y + 3, INK, false);
    }

    private void parentButton(GuiGraphics gui, String label, int x, boolean selected) {
        int y = topPos + PARENT_Y;
        gui.fill(x, y, x + PARENT_WIDTH, y + PARENT_HEIGHT, selected ? GREEN : 0xFFD7C398);
        gui.drawString(font, label, x + (PARENT_WIDTH - font.width(label)) / 2, y + 3, selected ? PAPER : INK, false);
    }

    private int parentX(int tray) {
        return leftPos + VerticalGreenhouseMenu.FIRST_TRAY_X + tray * VerticalGreenhouseMenu.TRAY_PITCH - 7;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int parentButtonAt(double x, double y) {
        if (!menu.fixedPair() || y < topPos + PARENT_Y || y >= topPos + PARENT_Y + PARENT_HEIGHT) return -1;
        for (int tray = 0; tray < VerticalGreenhouseBlockEntity.TRAYS; tray++) {
            int left = parentX(tray);
            if (x >= left && x < left + PARENT_WIDTH) return VerticalGreenhouseMenu.CHOOSE_A + tray;
            int right = left + PARENT_PITCH;
            if (x >= right && x < right + PARENT_WIDTH) return VerticalGreenhouseMenu.CHOOSE_B + tray;
        }
        return -1;
    }

    private boolean overControl(double x, double y, int offset) {
        return inside(x, y, leftPos + offset, topPos + CONTROL_Y, CONTROL_WIDTH, CONTROL_HEIGHT);
    }

    private boolean overWaterIntake(double x, double y) {
        return inside(x, y, leftPos + INTAKE_X, topPos + INTAKE_Y, INTAKE_WIDTH, INTAKE_HEIGHT);
    }

    private static void bar(GuiGraphics gui, int x, int y, int width, int progress, int maximum, int color) {
        gui.fill(x, y, x + width, y + 5, 0xFFBAA37C);
        int fill = Math.clamp(progress * width / maximum, 0, width);
        if (fill > 0) gui.fill(x, y, x + fill, y + 5, color);
    }

    private static String status(VerticalGreenhouseBlockEntity.Status status) {
        return switch (status) {
            case INCOMPLETE -> "Column incomplete";
            case OFF -> "Paused";
            case NO_SEEDS -> "Add seeds";
            case NO_HARVEST -> "No harvest rule";
            case NO_PAIR -> "Needs a pair";
            case NEED_WATER -> "Needs water";
            case OUTPUT_FULL -> "Output full";
            case RUNNING -> "Working";
            case BATCH_COMPLETE -> "Batch complete";
        };
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int action = parentButtonAt(x, y);
            if (overControl(x, y, GROW_X)) action = VerticalGreenhouseMenu.TOGGLE_GROW;
            else if (overControl(x, y, BREED_X)) action = VerticalGreenhouseMenu.TOGGLE_BREED;
            else if (overControl(x, y, PAIR_X)) action = VerticalGreenhouseMenu.TOGGLE_PAIRING;
            else if (overWaterIntake(x, y)) action = VerticalGreenhouseMenu.CYCLE_WATER_INTAKE;
            else if (inside(x, y, leftPos + BATCH_X, topPos + BATCH_Y, BATCH_WIDTH, 12)) action = VerticalGreenhouseMenu.CYCLE_BATCH;
            else if (menu.batchTarget() > 0 && inside(x, y, leftPos + BATCH_X, topPos + RESET_Y, BATCH_WIDTH, 12)) action = VerticalGreenhouseMenu.RESET_BATCH;
            else {
                int face = outputButtonAt(x, y);
                if (face >= 0) action = VerticalGreenhouseMenu.CYCLE_ROUTE + face;
            }
            if (action >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    private int outputButtonAt(double x, double y) {
        for (int face = 0; face < VerticalGreenhouseBlockEntity.ROUTE_COUNT; face++) {
            if (inside(x, y, leftPos + ROUTE_X, topPos + ROUTE_Y + face * ROUTE_PITCH, ROUTE_WIDTH, 12)) return face;
        }
        return -1;
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        renderTooltip(gui, mouseX, mouseY);
        int face = outputButtonAt(mouseX, mouseY);
        if (face >= 0) {
            String tip = face == 0 ? "Bottom hopper. Click to cycle crops, seeds, both or off."
                    : "Exports to the " + VerticalGreenhouseBlockEntity.outputFace(face).getName()
                            + " side. Click to choose what it sends to an adjacent inventory.";
            gui.renderTooltip(font, font.split(Component.literal(tip), 190), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, leftPos + BATCH_X, topPos + BATCH_Y, BATCH_WIDTH, 12)) {
            gui.renderTooltip(font, font.split(Component.literal("Click to set how many offspring to produce before breeding stops. Unlimited runs continuously."), 190), mouseX, mouseY);
            return;
        }
        if (menu.batchTarget() > 0 && inside(mouseX, mouseY, leftPos + BATCH_X, topPos + RESET_Y, BATCH_WIDTH, 12)) {
            gui.renderTooltip(font, Component.literal("Start a new batch at the same target"), mouseX, mouseY);
            return;
        }
        if (overWaterIntake(mouseX, mouseY)) {
            String tip = switch (menu.waterIntake()) {
                case DEMAND -> "Demand draws water when a job is available and the tank has less than one bucket.";
                case KEEP_FULL -> "Keep full draws one bucket per second when there is room, even while idle.";
                case OFF -> "Automatic intake is off. Buckets and external fluid pumps still work.";
            };
            gui.renderTooltip(font, font.split(Component.literal(tip + " Click to change mode."), 190), mouseX, mouseY);
            return;
        }
        if (overControl(mouseX, mouseY, PAIR_X)) {
            String tip = menu.fixedPair() ? "Fixed pair. Choose an A and B tray. Click here to rotate pairs."
                    : "Auto pair. Rotates compatible trays. Click here to choose parents.";
            gui.renderTooltip(font, font.split(Component.literal(tip), 180), mouseX, mouseY);
            return;
        }
        int parent = parentButtonAt(mouseX, mouseY);
        if (parent >= 0) {
            boolean second = parent >= VerticalGreenhouseMenu.CHOOSE_B;
            int tray = parent - (second ? VerticalGreenhouseMenu.CHOOSE_B : VerticalGreenhouseMenu.CHOOSE_A) + 1;
            boolean selected = (second ? menu.selectedB() : menu.selectedA()) == tray;
            String tip = (selected ? "Clear parent " : "Set parent ") + (second ? "B" : "A") + " on tray " + tray;
            gui.renderTooltip(font, Component.literal(tip), mouseX, mouseY);
            return;
        }
        if (mouseY < topPos + 106 || mouseY >= topPos + 116) return;
        for (int tray = 0; tray < VerticalGreenhouseBlockEntity.TRAYS; tray++) {
            int x = leftPos + VerticalGreenhouseMenu.FIRST_TRAY_X + tray * VerticalGreenhouseMenu.TRAY_PITCH - 7;
            if (mouseX < x || mouseX >= x + 30) continue;
            int progress = menu.trayProgress(tray);
            String tip = progress < 0 ? "Breeding only. This seed needs a harvest rule to grow."
                    : menu.getSlot(tray).hasItem() ? "Tray " + (tray + 1) + " growth: " + progress + "%" : "Add a seed to this tray";
            gui.renderTooltip(font, font.split(Component.literal(tip), 220), mouseX, mouseY);
            return;
        }
    }
}
