package com.agrievolution.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps slot, tooltip and widget coordinates together when a small screen needs scrolling. */
public abstract class ScrollableGardenScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final NativeGuiViewport viewport = new NativeGuiViewport();

    protected ScrollableGardenScreen(T menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Override protected void init() {
        super.init();
        viewport.init(width, height, imageWidth, imageHeight);
        leftPos = viewport.x();
        topPos = viewport.y();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (hasControlDown() && viewport.oversized()) {
            viewport.scroll(horizontal, vertical, hasShiftDown());
            moveViewport();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void moveViewport() {
        int dx = viewport.x() - leftPos, dy = viewport.y() - topPos;
        leftPos += dx;
        topPos += dy;
        for (var child : children()) if (child instanceof AbstractWidget widget) {
            widget.setX(widget.getX() + dx);
            widget.setY(widget.getY() + dy);
        }
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (viewport.keyPressed(key, modifiers)) { moveViewport(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        super.render(gui, mouseX, mouseY, delta);
        if (viewport.oversized()) {
            gui.fill(0, height - 14, width, height, 0xEF281D17);
            gui.drawCenteredString(font, "Ctrl+scroll or arrows  |  Shift: sideways", width / 2, height - 11, 0xFFF0DEB8);
        }
    }
}
