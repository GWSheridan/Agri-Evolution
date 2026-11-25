package com.agrievolution.client;

import com.agrievolution.client.journal.JournalScreen;
import net.minecraft.client.Minecraft;

import net.minecraft.world.item.ItemStack;

/** Only called behind Level#isClientSide; no client GUI loads on a dedicated server. */
public final class JournalClientScreens {
    private JournalClientScreens() {}

    public static void open(ItemStack journal) {
        Minecraft.getInstance().setScreen(
                new JournalScreen(journal.copy()));
    }
}
