package com.agrievolution.genetics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Explicit ancestry of a deliberately crossed seed, persisted through sowing and harvest. */
public final class HybridLineageStorage {
    private static final String KEY = "AgriEvolutionHybridLineage";
    private HybridLineageStorage() { }

    public record Lineage(CropSpecies first, CropSpecies second) {
        public String description() {
            return first.displayName() + " x " + second.displayName();
        }
    }

    public static Lineage read(CompoundTag tag) {
        if (!tag.contains(KEY, 10)) return null;
        CompoundTag data = tag.getCompound(KEY);
        CropSpecies first = species(data.getString("ParentA"));
        CropSpecies second = species(data.getString("ParentB"));
        return first != null && second != null && first != second ? new Lineage(first, second) : null;
    }

    public static void write(CompoundTag root, Lineage lineage) {
        if (lineage == null) { root.remove(KEY); return; }
        CompoundTag data = new CompoundTag();
        data.putString("ParentA", lineage.first().id());
        data.putString("ParentB", lineage.second().id());
        root.put(KEY, data);
    }

    public static Lineage fromSeed(ItemStack seed) {
        return read(seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public static void putOnSeed(ItemStack seed, Lineage lineage) {
        CompoundTag custom = seed.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        write(custom, lineage);
        seed.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    private static CropSpecies species(String id) {
        id = RetiredResourceCrops.canonical(id);
        for (CropSpecies candidate : CropSpecies.values()) {
            if (candidate.id().equals(id)) return candidate;
        }
        return null;
    }
}
