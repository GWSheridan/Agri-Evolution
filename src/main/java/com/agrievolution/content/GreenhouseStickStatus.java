package com.agrievolution.content;

/** One nonnegative menu value per plot. Finite lifetimes fit inside a signed short. */
public final class GreenhouseStickStatus {
    public static final int SIDE = 5;
    public static final int PLOT_COUNT = SIDE * SIDE;
    public static final int UNAVAILABLE = 0, NO_CROP = 1, UNSUPPORTED = 2, OBSIDIAN = 3;
    private static final int WOOD_BASE = 4;
    private static final int IRON_BASE = WOOD_BASE + CropSupportItem.lifetime(1) / 20 + 1;

    private GreenhouseStickStatus() {}

    public static int index(int across, int depth) {
        // Top is the back wall. The controller faces out, so its across axis is reversed for the viewer.
        return (SIDE - depth) * SIDE + (2 - across);
    }

    public static int encode(int tier, int ticks) {
        return switch (tier) {
            case 1, 2 -> (tier == 1 ? WOOD_BASE : IRON_BASE) + (Math.clamp(ticks, 0, CropSupportItem.lifetime(tier)) + 19) / 20;
            case 3 -> OBSIDIAN;
            default -> UNSUPPORTED;
        };
    }

    public static int tier(int value) {
        if (value == OBSIDIAN) return 3;
        if (value >= IRON_BASE) return 2;
        return value >= WOOD_BASE ? 1 : 0;
    }

    public static int seconds(int value) {
        if (value >= IRON_BASE) return value - IRON_BASE;
        return value >= WOOD_BASE ? value - WOOD_BASE : 0;
    }

    public static boolean dueSoon(int value) {
        return value >= WOOD_BASE && seconds(value) <= CropSupportItem.WEAR_WARNING_TICKS / 20;
    }
}
