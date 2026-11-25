package com.agrievolution.client.journal;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.genetics.SeedVariety;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SeedVarietyScreen extends Screen {
    private static final int INK = 0xFF342519, GREEN = 0xFF285947, PAPER = 0xFFF1DFB8;
    private final Screen parent;
    private final List<SeedVariety.Record> records;
    private int left, top, panelWidth, panelHeight, rows, page;

    SeedVarietyScreen(Screen parent, List<SeedVariety.Record> records) {
        super(Component.literal("Seed varieties"));
        this.parent = parent;
        this.records = records;
    }

    @Override protected void init() {
        panelWidth = Math.min(400, width - 16);
        panelHeight = Math.min(250, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rows = Math.max(1, (panelHeight - 64) / 42);
        page = Math.min(page, pages() - 1);
        button("<", 12, 24, () -> page = Math.max(0, page - 1));
        button(">", 39, 24, () -> page = Math.min(pages() - 1, page + 1));
        button("Back", panelWidth - 62, 50, this::onClose);
    }

    private int pages() { return Math.max(1, (records.size() + rows - 1) / rows); }

    private void button(String label, int x, int w, Runnable action) {
        addRenderableWidget(new Button(left + x, top + panelHeight - 26, w, 17,
                Component.literal(label), b -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float delta) {
                GardenGuiTheme.panel(gui, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? 0xFF397454 : GREEN);
                gui.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5, PAPER);
            }
        });
    }

    @Override public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.renderBackground(gui, mouseX, mouseY, delta);
        GardenGuiTheme.panel(gui, left, top, panelWidth, panelHeight, PAPER);
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        gui.drawString(font, title, left + 12, top + 13, GREEN, false);
        if (records.isEmpty()) {
            var hint = Component.literal("Rename a seed at an anvil. Plant it and use Grandmas Journal on the crop to record its variety.");
            int y = top + 36;
            for (var line : font.split(hint, panelWidth - 24)) {
                gui.drawString(font, line, left + 12, y, INK, false);
                y += 12;
            }
        }
        for (int row = 0; row < rows && page * rows + row < records.size(); row++) {
            var record = records.get(page * rows + row);
            int y = top + 32 + row * 42;
            JournalRenderer.currentCrop(gui, record.species(), left + 12, y, 24);
            var best = record.best();
            line(gui, record.name() + " / " + record.species().displayName(), y, GREEN);
            line(gui, "Best growth " + best.growth() + " / yield " + best.yield() + " / resilience " + best.resilience(), y + 11, INK);
            line(gui, "Parents: " + (record.parents() == null ? "Unrecorded" : record.parents().description()), y + 22, INK);
            if (mouseX >= left + 12 && mouseX < left + panelWidth - 12 && mouseY >= y && mouseY < y + 33) {
                gui.renderTooltip(font, font.split(Component.literal(record.name() + " / " + record.species().displayName()
                        + (record.parents() == null ? "" : "\nParents: " + record.parents().description())), panelWidth - 24), mouseX, mouseY);
            }
        }
        gui.drawString(font, (page + 1) + " / " + pages(), left + 72, top + panelHeight - 21, INK, false);
    }

    private void line(GuiGraphics gui, String text, int y, int colour) {
        gui.drawString(font, font.plainSubstrByWidth(text, panelWidth - 53), left + 41, y, colour, false);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
