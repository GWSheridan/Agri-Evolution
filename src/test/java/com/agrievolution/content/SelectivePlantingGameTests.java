package com.agrievolution.content;
import com.agrievolution.genetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class SelectivePlantingGameTests {
    private static BotanicalGreenhouseBlockEntity house(){
        var house=new BotanicalGreenhouseBlockEntity(BlockPos.ZERO,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState());
        house.data.set(19,1);house.data.set(26,1);return house;
    }
    private static ItemStack seed(CropSpecies species,int count,int g,int y,int r){var seed=new ItemStack(AEItems.seedFor(species),count);GenomeStorage.putOnSeed(seed,new CropGenome(g,y,r));return seed;}
    private static int choose(BotanicalGreenhouseBlockEntity house)throws Exception{var m=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("chooseSeedBankSlot");m.setAccessible(true);return (int)m.invoke(house);}
    @GameTest(template="empty") public static void speciesAndEachGeneFilterSelection(GameTestHelper h)throws Exception{
        var house=house();var good=seed(CropSpecies.COPPER,10,60,70,80);house.seedBank().setItem(0,good);
        house.togglePlantingSpecies(0);h.assertTrue(house.seedBankSettings.get(20)==CropSpecies.COPPER.ordinal()+1,"Locks current species");
        house.seedBank().setItem(0,seed(CropSpecies.IRON,10,100,100,100));h.assertTrue(choose(house)==-1,"Wrong species excluded despite stronger genetics");
        house.seedBank().setItem(0,good);
        for(int gene=0;gene<3;gene++){
            house.seedBankSettings.set(29+gene,new int[]{61,71,81}[gene]);h.assertTrue(choose(house)==-1,"Each genetic threshold is enforced");house.seedBankSettings.set(29+gene,new int[]{60,70,80}[gene]);
        }
        h.assertTrue(choose(house)==0&&good.getCount()==10,"Inclusive minimum and selection does not consume seeds");
        house.togglePlantingSpecies(0);house.seedBank().setItem(0,seed(CropSpecies.IRON,10,100,100,100));h.assertTrue(choose(house)==0,"Cleared species lock permits other crops");h.succeed();
    }
    @GameTest(template="empty") public static void reserveAndLegacySettingsRemainSafe(GameTestHelper h)throws Exception{
        var house=house();house.seedBank().setItem(0,seed(CropSpecies.COPPER,33,100,100,100));house.seedBankSettings.set(18,32);
        h.assertTrue(choose(house)==0,"Above reserve is eligible");house.seedBank().getItem(0).shrink(1);h.assertTrue(choose(house)==-1,"Reserve boundary blocks planting");
        house.seedBankSettings.set(18,64);h.assertTrue(choose(house)==-1,"Full stack reserve supported");
        house.seedBankSettings.set(18,0);house.toggleSeedSlot(0);h.assertTrue(choose(house)==-1,"Existing disabled slot still applies");
        house.toggleSeedSlot(0);house.toggleResourcePlanting();h.assertTrue(choose(house)==-1,"Existing category filter still applies");h.succeed();
    }
    @GameTest(template="empty") public static void rulesPersistWithStableIdsAndSafeDefaults(GameTestHelper h){
        var house=house();house.seedBank().setItem(0,seed(CropSpecies.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,64,50,60,70));house.togglePlantingSpecies(0);
        house.seedBankSettings.set(18,64);house.seedBankSettings.set(29,50);house.seedBankSettings.set(30,60);house.seedBankSettings.set(31,70);
        var tag=new CompoundTag();house.saveAdditional(tag,h.getLevel().registryAccess());
        h.assertTrue(tag.getCompound("SelectivePlantingSpecies").getString("Slot0").equals("silence_armor_trim_smithing_template"),"Stable species ID saved");
        var copy=house();copy.loadAdditional(tag,h.getLevel().registryAccess());for(int i=18;i<32;i++)h.assertTrue(copy.seedBankSettings.get(i)==house.seedBankSettings.get(i),"Rule synchronized and persisted "+i);
        copy.loadAdditional(new CompoundTag(),h.getLevel().registryAccess());for(int i=20;i<32;i++)h.assertTrue(copy.seedBankSettings.get(i)==0,"Old saves default to no filters");
        copy.changeMinimumPlantingGene(0,10);h.assertTrue(copy.seedBankSettings.get(29)==0,"Upgrade required to edit rules");
        tag.putIntArray("SelectivePlantingMinimumGenes",new int[]{-10,500,50});copy.loadAdditional(tag,h.getLevel().registryAccess());h.assertTrue(copy.seedBankSettings.get(29)==0&&copy.seedBankSettings.get(30)==100,"Invalid NBT thresholds clamped");h.succeed();
    }
    @GameTest(template="empty") public static void actualPlacementPreservesGenesAndStopsAtReserve(GameTestHelper h)throws Exception{
        var house=house();var pos=h.absolutePos(new BlockPos(3,180,3));var level=h.getLevel();
        level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(),2);
        level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        var stack=seed(CropSpecies.COPPER,33,60,70,80);house.seedBank().setItem(0,stack);house.togglePlantingSpecies(0);house.seedBankSettings.set(18,32);
        var method=BotanicalGreenhouseBlockEntity.class.getDeclaredMethod("placeBankSeed",net.minecraft.server.level.ServerLevel.class,BlockPos.class,int.class,int.class);method.setAccessible(true);
        house.seedBankSettings.set(29,70);h.assertFalse((boolean)method.invoke(house,level,pos,0,2),"Below-minimum seed cannot be placed");h.assertTrue(stack.getCount()==33,"Rejected planting consumes nothing");
        house.seedBankSettings.set(29,60);h.assertTrue((boolean)method.invoke(house,level,pos,0,2),"Eligible seed planted");
        h.assertTrue(((AECropBlockEntity)level.getBlockEntity(pos)).genome().equals(new CropGenome(60,70,80))&&CropSupportItem.tier(level.getBlockState(pos))==2,"Genes and support preserved");
        level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        h.assertFalse((boolean)method.invoke(house,level,pos,0,2),"Cannot plant below reserve");h.assertTrue(stack.getCount()==32,"Reserved seeds retained");h.succeed();
    }
    @GameTest(template="empty") public static void diagnosticsMatchSelectionAndReserve(GameTestHelper h)throws Exception{
        var house=house();
        h.assertTrue(house.plantingExclusions(0)==1,"Empty slot diagnosed");
        house.seedBank().setItem(0,seed(CropSpecies.COPPER,40,50,60,70));house.togglePlantingSpecies(0);
        house.seedBankSettings.set(18,32);
        h.assertTrue(house.eligibleSeedCount(0)==8&&choose(house)==0,"Only unreserved seeds count");
        house.seedBankSettings.set(29,60);house.seedBankSettings.set(30,70);house.seedBankSettings.set(31,80);
        h.assertTrue(house.plantingExclusions(0)==112&&choose(house)==-1,"All three gene failures reported together");
        house.seedBankSettings.set(18,64);house.toggleSeedSlot(0);house.toggleResourcePlanting();
        house.seedBank().setItem(0,seed(CropSpecies.IRON,40,50,60,70));
        h.assertTrue(house.plantingExclusions(0)==254,"All simultaneous exclusions retained");
        h.assertTrue(house.seedBankSettings.get(32)==254&&house.seedBankSettings.get(41)==0,"Diagnostics synced to menu");
        h.assertTrue(com.agrievolution.farming.PlantingDiagnostics.reasons(254).size()==7,"Every exclusion has a message");
        h.assertTrue(house.seedBank().getItem(0).getCount()==40,"Diagnostics never consume seeds");h.succeed();
    }

    @GameTest(template="empty")
    public static void plantingPresetsRestoreRulesWithoutMovingSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        var position = helper.absolutePos(new BlockPos(3, 50, 3));
        level.setBlock(position, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState(), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) level.getBlockEntity(position);
        greenhouse.data.set(19, 1);
        greenhouse.data.set(26, 1);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 1.5);
        var menu = new BotanicalGreenhouseSeedBankMenu(1, player.getInventory(), greenhouse);
        int save = BotanicalGreenhouseSeedBankMenu.SAVE_PRESET_START;
        int load = BotanicalGreenhouseSeedBankMenu.LOAD_PRESET_START;
        helper.assertFalse(menu.clickMenuButton(player, load), "Loading an empty preset does nothing");

        var copper = seed(CropSpecies.COPPER, 10, 70, 60, 50);
        greenhouse.seedBank().setItem(0, copper);
        greenhouse.togglePlantingSpecies(0);
        greenhouse.changeMinimumPlantingGene(0, 70);
        helper.assertTrue(menu.clickMenuButton(player, save) && menu.presetMatches(0), "Save A captures the current rules");
        greenhouse.togglePlantingSpecies(0);
        greenhouse.changeMinimumPlantingGene(0, 20);
        helper.assertTrue(menu.clickMenuButton(player, save + 1) && menu.presetMatches(1) && !menu.presetMatches(0), "B is independent of A");
        helper.assertTrue(menu.eligibleSeeds(0) == 0, "Preset B requires a higher Growth value");
        greenhouse.cycleSeedReserve();
        helper.assertTrue(menu.clickMenuButton(player, load), "Load A through the normal menu action");
        helper.assertTrue(menu.plantingSpecies(0) == CropSpecies.COPPER.ordinal() + 1 && menu.minimumGene(0) == 70,
                "Load restores the species lock and threshold together");
        helper.assertTrue(greenhouse.seedBank().getItem(0).getCount() == 10 && menu.reservePerSlot() == 1 && menu.eligibleSeeds(0) == 9,
                "Reload updates eligibility and leaves inventory and reserve intact");

        var tag = new CompoundTag();
        greenhouse.saveAdditional(tag, level.registryAccess());
        helper.assertTrue(tag.getCompound("PlantingPresets").getCompound("Preset0").getCompound("Species").getString("Slot0").equals("copper"),
                "Preset species use stable IDs in saves");
        var restored = house();
        restored.loadAdditional(tag, level.registryAccess());
        helper.assertTrue(restored.loadPlantingPreset(1) && restored.seedBankSettings.get(29) == 90 && restored.seedBankSettings.get(20) == 0,
                "B survives the save round trip");
        helper.assertTrue(restored.loadPlantingPreset(0) && restored.seedBankSettings.get(29) == 70, "A also survives");
        tag.remove("PlantingPresets");
        restored.loadAdditional(tag, level.registryAccess());
        helper.assertFalse(restored.loadPlantingPreset(0), "Old saves have empty presets");
        helper.succeed();
    }
}
