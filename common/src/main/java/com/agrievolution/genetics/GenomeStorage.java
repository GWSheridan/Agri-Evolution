package com.agrievolution.genetics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** NBT bridge for immutable CropGenome. Gameplay decisions are made server-side. */
public final class GenomeStorage {
    private static final String GENOME_KEY = "AgriEvolutionGenome";
    private static final String GROWTH = "Growth";
    private static final String YIELD = "Yield";
    private static final String RESILIENCE = "Resilience";

    private GenomeStorage() {}

    public static CompoundTag writeGenome(CropGenome genome) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(GROWTH, genome.growth());
        tag.putInt(YIELD, genome.yield());
        tag.putInt(RESILIENCE, genome.resilience());
        return tag;
    }

    public static CropGenome readGenome(CompoundTag tag) {
        if (!tag.contains(GROWTH) || !tag.contains(YIELD) || !tag.contains(RESILIENCE)) {
            return CropGenome.wild();
        }
        return new CropGenome(tag.getInt(GROWTH), tag.getInt(YIELD), tag.getInt(RESILIENCE));
    }

    public static CropGenome fromSeed(ItemStack stack) {
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!custom.contains(GENOME_KEY, 10)) return CropGenome.wild();
        return readGenome(custom.getCompound(GENOME_KEY));
    }

    public static void putOnSeed(ItemStack stack, CropGenome genome) {
        // Copy rather than overwriting other custom item data.
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        custom.put(GENOME_KEY, writeGenome(genome));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }
}
