package com.agrievolution.platform;

public interface ChunkAwareBlockEntity {
    default void onLoad() {}

    default void onChunkUnloaded() {}
}
