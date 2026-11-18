package com.agrievolution.client.journal;

import com.agrievolution.client.GardenGuiTheme;
import com.agrievolution.farming.CompanionPlanting;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class CompanionTrialsScreen extends Screen {
    private final Screen parent;
    private final Set<String> discovered;
    private int page, x, y, w;

    CompanionTrialsScreen(Screen parent, Set<String> discovered) {
        super(Component.literal("Companion planting"));
        this.parent = parent;
        this.discovered = discovered;
    }

    @Override protected void init() {
        w = Math.min(width - 12, 308);
        x = (width - w) / 2;
        y = (height - 158) / 2;
        addRenderableWidget(GardenGuiTheme.button(Component.literal("<"), x + 12, y + 128, 22, 18, b -> move(-1)));
        addRenderableWidget(GardenGuiTheme.button(Component.literal(">"), x + 38, y + 128, 22, 18, b -> move(1)));
        addRenderableWidget(GardenGuiTheme.button(Component.literal("Back"), x + w - 62, y + 128, 50, 18, b -> onClose()));
    }

    private void move(int change) { page = Math.floorMod(page + change, CompanionPlanting.TRIALS.size()); }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        GardenGuiTheme.panel(g, x, y, w, 158, 0xffeeddb6);
        g.drawString(font, title, x + 12, y + 10, 0xff214a36, false);
        var trial = CompanionPlanting.TRIALS.get(page);
        boolean known = discovered.contains(trial.id());
        int middle = x + w / 2;
        int leftCrop = middle - 54, rightCrop = middle + 54;
        GardenGuiTheme.panel(g, middle - 82, y + 27, 164, 45, 0xffab784b);
        if (known) {
            JournalRenderer.currentCrop(g, trial.helper(), leftCrop - 20, y + 29, 40);
            JournalRenderer.currentCrop(g, trial.beneficiary(), rightCrop - 20, y + 29, 40);
            GardenGuiTheme.center(g, trial.helper().displayName(), leftCrop, y + 77, 0xff35251b);
            GardenGuiTheme.center(g, trial.beneficiary().displayName(), rightCrop, y + 77, 0xff35251b);
        } else {
            GardenGuiTheme.center(g, "?", leftCrop, y + 46, 0xfff5e7bc);
            JournalRenderer.currentCrop(g, trial.beneficiary(), rightCrop - 20, y + 29, 40);
            GardenGuiTheme.center(g, trial.beneficiary().displayName(), rightCrop, y + 77, 0xff35251b);
        }
        String detail = known ? trial.bonus() + " Plant the crops side by side."
                : "Try a mature herb, flower or legume beside " + trial.beneficiary().displayName() + ". Use this journal on either crop to record a pairing.";
        int row = y + 92;
        for (var line : font.split(Component.literal(detail), w - 22)) {
            g.drawString(font, line, x + 11, row, 0xff35251b, false);
            row += 10;
        }
        GardenGuiTheme.center(g, (page + 1) + " / " + CompanionPlanting.TRIALS.size() + " · " + discovered.size()
                + " discovered", middle, y + 133, 0xff214a36);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
