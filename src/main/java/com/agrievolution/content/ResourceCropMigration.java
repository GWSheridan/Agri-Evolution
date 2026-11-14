package com.agrievolution.content;

import com.agrievolution.genetics.RetiredResourceCrops;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Save compatibility for retired compressed-resource crops, without registering duplicates. */
public final class ResourceCropMigration {
    private ResourceCropMigration() {}
    public static void aliases(DeferredRegister<?> registry, String suffix) {
        RetiredResourceCrops.ALIASES.forEach((oldId,newId)->registry.addAlias(
            ResourceLocation.fromNamespaceAndPath("agri_evolution",oldId+suffix),
            ResourceLocation.fromNamespaceAndPath("agri_evolution",newId+suffix)));
    }
    public static void journal(CompoundTag root) {
        if(root.contains("AgriDiscoveredSpecies",Tag.TAG_LIST)) {
            var old=root.getList("AgriDiscoveredSpecies",Tag.TAG_STRING);
            var ids=new java.util.LinkedHashSet<String>();
            for(int i=0;i<old.size();i++)ids.add(RetiredResourceCrops.canonical(old.getString(i)));
            var updated=new ListTag();ids.forEach(id->updated.add(StringTag.valueOf(id)));
            root.put("AgriDiscoveredSpecies",updated);
        }
        if(root.contains("AgriSpeciesRecords",Tag.TAG_COMPOUND)) {
            var records=root.getCompound("AgriSpeciesRecords");
            RetiredResourceCrops.ALIASES.forEach((oldId,newId)->{
                if(records.contains(oldId)) {
                    if(!records.contains(newId))records.put(newId,records.getCompound(oldId).copy());
                    records.remove(oldId);
                }
            });
        }
    }
}
