package com.agrievolution.client;

/** Integer-aligned scrolling for screens larger than Minecraft's logical viewport. */
public final class NativeGuiViewport {
    private int x, y, width, height, contentWidth, contentHeight;

    public void init(int width, int height, int contentWidth, int contentHeight) {
        this.width = width;
        this.height = height - 16;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        x = contentWidth > width - 8 ? 4 : (width - contentWidth) / 2;
        y = contentHeight > height - 8 ? 4 : (height - contentHeight) / 2;
    }

    public int x() { return x; }
    public int y() { return y; }
    public boolean oversized() { return contentWidth > width - 8 || contentHeight > height - 8; }

    public boolean scroll(double horizontal, double vertical, boolean sideways) {
        if (!oversized()) return false;
        int oldX = x, oldY = y;
        if (contentWidth > width - 8) {
            double amount = horizontal != 0 ? horizontal : sideways || contentHeight <= height - 8 ? vertical : 0;
            x = Math.clamp(x + (int) Math.round(amount * 24), width - contentWidth - 4, 4);
        }
        if (!sideways && contentHeight > height - 8)
            y = Math.clamp(y + (int) Math.round(vertical * 24), height - contentHeight - 4, 4);
        return oldX != x || oldY != y;
    }

    public boolean keyPressed(int key, int modifiers) {
        if (!oversized() || (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) == 0) return false;
        return switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> { scroll(-1, 0, true); yield true; }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> { scroll(1, 0, true); yield true; }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> {
                if (contentHeight > height - 8) scroll(0, -1, false);
                yield true;
            }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> {
                if (contentHeight > height - 8) scroll(0, 1, false);
                yield true;
            }
            default -> false;
        };
    }
}
