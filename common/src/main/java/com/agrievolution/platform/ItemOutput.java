package com.agrievolution.platform;

import net.minecraft.world.item.ItemStack;

public interface ItemOutput {
    int slots();

    ItemStack insert(int slot, ItemStack stack, boolean simulate);

    ItemStack insertStacked(ItemStack stack);
}
