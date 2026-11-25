package com.agrievolution.genetics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Separate NBT key so existing genetic data and modded item components are retained. */
public final class RareTraitStorage {
    private static final String KEY = "AgriEvolutionRareTrait";
    private RareTraitStorage() { }
    public static RareTrait fromSeed(ItemStack seed) {
        CompoundTag custom = seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return RareTrait.fromName(custom.getString(KEY));
    }
    public static void putOnSeed(ItemStack seed, RareTrait trait) {
        CompoundTag custom = seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (trait == RareTrait.NONE) custom.remove(KEY);
        else custom.putString(KEY, trait.name());
        seed.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }
}
