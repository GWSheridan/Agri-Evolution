package com.agrievolution.content;

/** Two bits per plot. Seven plots fit in each signed-short menu value. */
public enum GreenhouseWeedStatus {
    UNAVAILABLE, NO_CROP, CLEAR, WEEDY;

    private static final GreenhouseWeedStatus[] STATUSES = values();
    private static final int PLOTS_PER_VALUE = 7;
    public static final int DATA_COUNT = (GreenhouseStickStatus.PLOT_COUNT + PLOTS_PER_VALUE - 1) / PLOTS_PER_VALUE;

    public static int dataIndex(int plot) {
        return plot / PLOTS_PER_VALUE;
    }

    public int bits(int plot) {
        return ordinal() << shift(plot);
    }

    public static GreenhouseWeedStatus decode(int value, int plot) {
        return STATUSES[(value >>> shift(plot)) & 3];
    }

    private static int shift(int plot) {
        return 2 * (plot % PLOTS_PER_VALUE);
    }
}
