package com.agrievolution.client.journal;

import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.SeedVariety;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.EnumMap;
import java.util.Map;

/**
 * Read-only view of the existing 0.4.0+ journal NBT format.
 * Migrates retired species in its private copy; the original item remains untouched.
 */
public final class JournalSnapshot {
    private static final String DISCOVERED = "AgriDiscoveredSpecies";
    private static final String RECORDS = "AgriSpeciesRecords";
    private static final String SELECTED = "AgriSelectedParent";

    public record Entry(CropSpecies species, boolean discovered,
                        boolean hasGenetics, int growth, int yield, int resilience) {}

    private final Map<CropSpecies, Entry> entries;
    private final boolean parentSelected;
    private final int discoveredCount;
    private final java.util.Set<RareTrait> rareDiscovered;
    private final java.util.List<SeedVariety.Record> varieties;

    private JournalSnapshot(Map<CropSpecies, Entry> entries, boolean parentSelected,
                            int discoveredCount, java.util.Set<RareTrait> rareDiscovered, java.util.List<SeedVariety.Record> varieties) {
        this.entries = Map.copyOf(entries);
        this.parentSelected = parentSelected;
        this.discoveredCount = discoveredCount;
        this.rareDiscovered = java.util.Set.copyOf(rareDiscovered);
        this.varieties = varieties;
    }

    public static JournalSnapshot from(ItemStack journal) {
        CompoundTag root = journal.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        com.agrievolution.content.ResourceCropMigration.journal(root);
        ListTag discovered = root.getList(DISCOVERED, Tag.TAG_STRING);
        CompoundTag best = root.getCompound(RECORDS);
        EnumMap<CropSpecies, Entry> entries = new EnumMap<>(CropSpecies.class);
        int total = 0;

        for (CropSpecies species : CropSpecies.values()) {
            boolean known = false;
            for (int i = 0; i < discovered.size(); i++) {
                if (species.id().equals(discovered.getString(i))) {
                    known = true;
                    break;
                }
            }
            if (known) total++;
            CompoundTag record = best.getCompound(species.id());
            boolean hasGenes = known && record.contains("Growth", Tag.TAG_INT);
            entries.put(species, new Entry(species, known, hasGenes,
                    hasGenes ? record.getInt("Growth") : 0,
                    hasGenes ? record.getInt("Yield") : 0,
                    hasGenes ? record.getInt("Resilience") : 0));
        }
        java.util.EnumSet<RareTrait> rare = java.util.EnumSet.noneOf(RareTrait.class);
        ListTag found = root.getList("AgriRareGenes", Tag.TAG_STRING);
        for (int i = 0; i < found.size(); i++) {
            RareTrait trait = RareTrait.fromName(found.getString(i));
            if (trait != RareTrait.NONE) rare.add(trait);
        }
        return new JournalSnapshot(entries, root.contains(SELECTED, Tag.TAG_COMPOUND), total, rare, SeedVariety.records(root));
    }

    public Entry entry(CropSpecies species) { return entries.get(species); }
    public int discoveredCount() { return discoveredCount; }
    public int speciesCount() { return CropSpecies.values().length; }
    public boolean parentSelected() { return parentSelected; }
    public int rareDiscoveredCount() { return rareDiscovered.size(); }
    public boolean rareDiscovered(RareTrait trait) { return rareDiscovered.contains(trait); }
    public java.util.List<SeedVariety.Record> varieties() { return varieties; }
}
