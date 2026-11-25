package com.agrievolution.platform;

import net.minecraft.resources.ResourceLocation;

public final class ModInfo {
    public static final String ID = "agri_evolution";

    private ModInfo() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}
