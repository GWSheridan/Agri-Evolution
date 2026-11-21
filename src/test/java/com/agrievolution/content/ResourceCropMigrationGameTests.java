package com.agrievolution.content;

import com.agrievolution.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ResourceCropMigrationGameTests {
    @GameTest(template="empty") public static void oldSeedAndPlantIdsDeserialize(GameTestHelper h) {
        for(var entry:RetiredResourceCrops.ALIASES.entrySet()) {
            var species=RetiredResourceCrops.species(entry.getValue());
            var seed=AEItems.seedFor(species);
            var original=new ItemStack(seed,17);
            var genes=new CropGenome(42,53,64);GenomeStorage.putOnSeed(original,genes);
            var tag=(CompoundTag)original.save(h.getLevel().registryAccess());
            var oldId=ResourceLocation.fromNamespaceAndPath("agri_evolution",entry.getKey()+"_seed");
            tag.putString("id",oldId.toString());
            var restored=ItemStack.parse(h.getLevel().registryAccess(),tag).orElseThrow();
            h.assertTrue(restored.is(seed)&&restored.getCount()==17,"Old seed resolves to base resource: "+entry);
            h.assertTrue(restored.getComponents().equals(original.getComponents()),"Seed genetics/components survive alias");
            h.assertFalse(BuiltInRegistries.ITEM.keySet().contains(oldId),"No duplicate seed registration");
            var blockTag=new CompoundTag();blockTag.putString("Name","agri_evolution:"+entry.getKey()+"_crop");
            var props=new CompoundTag();props.putString("age","5");props.putString("supported","true");props.putString("support_tier","2");blockTag.put("Properties",props);
            var state=BlockState.CODEC.parse(NbtOps.INSTANCE,blockTag).getOrThrow();
            h.assertTrue(state.is(((SpeciesSeedItem)seed).cropBlock())&&state.getValue(AgriCropBlock.AGE)==5,"Old plant retains age");
            h.assertTrue(CropSupportItem.tier(state)==2,"Old plant retains support tier");
        }h.succeed();
    }
    @GameTest(template="empty") public static void oldJournalAndLineageKeepDiscoveries(GameTestHelper h) {
        var root=new CompoundTag();var discoveries=new ListTag();discoveries.add(StringTag.valueOf("copper_block"));discoveries.add(StringTag.valueOf("copper"));root.put("AgriDiscoveredSpecies",discoveries);
        var records=new CompoundTag();var record=new CompoundTag();record.putInt("Growth",72);records.put("copper_block",record);root.put("AgriSpeciesRecords",records);
        var lineage=new CompoundTag();lineage.putString("ParentA","magma_block");lineage.putString("ParentB","shroomlight");root.put("AgriEvolutionHybridLineage",lineage);
        ResourceCropMigration.journal(root);ResourceCropMigration.journal(root);
        h.assertTrue(root.getList("AgriDiscoveredSpecies",8).size()==1&&root.getList("AgriDiscoveredSpecies",8).getString(0).equals("copper"),"Discovery migration is deduplicated and idempotent");
        h.assertTrue(root.getCompound("AgriSpeciesRecords").getCompound("copper").getInt("Growth")==72,"Journal genetics preserved");
        h.assertTrue(HybridLineageStorage.read(root).first()==CropSpecies.MAGMA_CREAM,"Retired ancestry resolves");h.succeed();
    }
    private static CompoundTag overlappingJournal(String oldId, String currentId) {
        var root = new CompoundTag();
        var discoveries = new ListTag();
        discoveries.add(StringTag.valueOf(oldId));
        discoveries.add(StringTag.valueOf(currentId));
        root.put("AgriDiscoveredSpecies", discoveries);
        var records = new CompoundTag();
        var retired = new CompoundTag();
        retired.putInt("Growth", 80);
        retired.putInt("Yield", 20);
        retired.putInt("Resilience", 65);
        retired.putString("LegacyNote", "Retained research");
        retired.putString("SharedNote", "Older entry");
        var current = new CompoundTag();
        current.putInt("Growth", 40);
        current.putInt("Yield", 90);
        current.putInt("Resilience", 5);
        current.putString("SharedNote", "Current entry");
        records.put(oldId, retired);
        records.put(currentId, current);
        root.put("AgriSpeciesRecords", records);
        return root;
    }
    @GameTest(template="empty") public static void overlappingJournalRecordsKeepEachBestTrait(GameTestHelper h) {
        for (var alias : RetiredResourceCrops.ALIASES.entrySet()) {
            var root = overlappingJournal(alias.getKey(), alias.getValue());
            ResourceCropMigration.journal(root);
            var records = root.getCompound("AgriSpeciesRecords");
            var merged = records.getCompound(alias.getValue());
            h.assertTrue(merged.getInt("Growth") == 80 && merged.getInt("Yield") == 90 && merged.getInt("Resilience") == 65,
                    "Merging " + alias + " retains the strongest observed value of each trait");
            h.assertTrue(!records.contains(alias.getKey()) && root.getList("AgriDiscoveredSpecies", Tag.TAG_STRING).size() == 1,
                    "Retired records and discoveries merge without duplicate species");
            h.assertTrue(merged.getString("LegacyNote").equals("Retained research") && merged.getString("SharedNote").equals("Current entry"),
                    "Unrelated old fields survive while current fields take precedence");
            var once = root.copy();
            ResourceCropMigration.journal(root);
            h.assertTrue(root.equals(once), "Journal migration is idempotent");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void savedJournalPreviewKeepsBestTraitsWithoutEditingItem(GameTestHelper h) {
        var journal = new ItemStack(AEItems.BREEDING_JOURNAL.get());
        journal.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(overlappingJournal("copper_block", "copper")));
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) journal.save(h.getLevel().registryAccess()));
        var before = restored.copy();
        var entry = com.agrievolution.client.journal.JournalSnapshot.from(restored).entry(CropSpecies.COPPER);
        h.assertTrue(entry.discovered() && entry.hasGenetics() && entry.growth() == 80 && entry.yield() == 90 && entry.resilience() == 65,
                "A saved journal displays the strongest traits from overlapping old and current records");
        h.assertTrue(ItemStack.matches(before, restored), "Reading a journal does not rewrite its saved item data");
        h.succeed();
    }
    @GameTest(template="empty") public static void greenhouseSavesStableSpeciesIds(GameTestHelper h) {
        var state=AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState();
        var house=new BotanicalGreenhouseBlockEntity(BlockPos.ZERO,state);var tag=new CompoundTag();
        // Original 0.68 final species ordinal+1; removal must not turn this into a new crop.
        tag.putInt("MonitoringLastSpecies",251);tag.putInt("MonitoringPlantedSpecies",251);
        house.loadAdditional(tag,h.getLevel().registryAccess());
        h.assertTrue(house.data.get(37)==CropSpecies.ENCHANTED_GOLDEN_APPLE.ordinal()+1&&house.data.get(42)==CropSpecies.ENCHANTED_GOLDEN_APPLE.ordinal()+1,"Legacy greenhouse ordinal migration");
        var saved=new CompoundTag();house.saveAdditional(saved,h.getLevel().registryAccess());
        h.assertTrue(saved.getString("MonitoringLastSpeciesId").equals("enchanted_golden_apple"),"Stable ID persistence");
        var copy=new BotanicalGreenhouseBlockEntity(BlockPos.ZERO,state);copy.loadAdditional(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.data.get(37)==house.data.get(37)&&copy.data.get(42)==house.data.get(42),"Stable IDs round trip");h.succeed();
    }
}
