package com.agrievolution.client;

import com.agrievolution.client.analyzer.SeedAnalyzerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

/** Called only inside the client-side branch of SeedAnalyzerBlock's interaction. */
public final class SeedAnalyzerClientScreens {
    private SeedAnalyzerClientScreens() {}

    public static void open(ItemStack stack) {
        open(stack, InteractionHand.MAIN_HAND);
    }

    public static void open(ItemStack stack, InteractionHand hand) {
        int heldSlot = hand == InteractionHand.MAIN_HAND && Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getInventory().selected : -1;
        Minecraft.getInstance().setScreen(new SeedAnalyzerScreen(stack.copy(), heldSlot));
    }
}
