package com.agrievolution.client;

import com.agrievolution.client.soil.SoilSamplerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Isolated client-only entry point, invoked only by the client-side item use. */
public final class SoilSamplerClientScreens {
    private SoilSamplerClientScreens() {}

    public static void open(ItemStack sampler) {
        Minecraft.getInstance().setScreen(new SoilSamplerScreen(sampler.copy()));
    }
}
