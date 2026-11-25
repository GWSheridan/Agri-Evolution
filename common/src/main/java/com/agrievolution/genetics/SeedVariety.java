package com.agrievolution.genetics;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;

public final class SeedVariety {
    private static final String RECORDS = "AgriVarieties";
    private static final int RECORD_LIMIT = 96;
    public record Record(String name, CropSpecies species, CropGenome best, HybridLineageStorage.Lineage parents) {}
    private SeedVariety() {}

    public static String clean(String name) {
        String value = name.strip();
        return value.substring(0, value.offsetByCodePoints(0, Math.min(50, value.codePointCount(0, value.length()))));
    }

    public static String name(ItemStack seed) {
        var custom = seed.get(DataComponents.CUSTOM_NAME);
        return custom == null ? "" : clean(custom.getString());
    }

    public static void apply(ItemStack seed, String name) {
        if (!name.isEmpty()) seed.set(DataComponents.CUSTOM_NAME, Component.literal(name));
    }

    public static void inherit(ItemStack result, ItemStack first, ItemStack second) {
        String name = name(first);
        if (!name.isEmpty() && result.is(first.getItem()) && result.is(second.getItem()) && name.equals(name(second))) {
            apply(result, name);
            var lineage = HybridLineageStorage.fromSeed(first);
            if (lineage != null && lineage.equals(HybridLineageStorage.fromSeed(second))) {
                HybridLineageStorage.putOnSeed(result, lineage);
            }
        }
    }

    public static void record(CompoundTag journal, String name, CropSpecies species, CropGenome genes,
                              HybridLineageStorage.Lineage parents) {
        if (name.isEmpty()) return;
        var entries = journal.getList(RECORDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            if (!name.equals(entry.getString("Name")) || !species.id().equals(entry.getString("Species"))) continue;
            var old = GenomeStorage.readGenome(entry.getCompound("Best"));
            genes = new CropGenome(Math.max(genes.growth(), old.growth()), Math.max(genes.yield(), old.yield()),
                    Math.max(genes.resilience(), old.resilience()));
            if (parents == null) parents = HybridLineageStorage.read(entry);
            entries.remove(i);
            break;
        }
        var entry = new CompoundTag();
        entry.putString("Name", name);
        entry.putString("Species", species.id());
        entry.put("Best", GenomeStorage.writeGenome(genes));
        HybridLineageStorage.write(entry, parents);
        entries.add(0, entry);
        while (entries.size() > RECORD_LIMIT) entries.removeLast();
        journal.put(RECORDS, entries);
    }

    public static List<Record> records(CompoundTag journal) {
        var results = new ArrayList<Record>();
        var entries = journal.getList(RECORDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(RECORD_LIMIT, entries.size()); i++) {
            var entry = entries.getCompound(i);
            String name = clean(entry.getString("Name"));
            var species = RetiredResourceCrops.species(entry.getString("Species"));
            if (!name.isEmpty() && species != null) results.add(new Record(name, species,
                    GenomeStorage.readGenome(entry.getCompound("Best")), HybridLineageStorage.read(entry)));
        }
        return List.copyOf(results);
    }
}
