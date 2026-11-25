package com.agrievolution.content;

public interface WaterTank {
    int water();

    int capacity();

    boolean complete();

    int fillWater(int maximum, boolean execute);

    default int drainWater(int maximum, boolean execute) { return 0; }
}
