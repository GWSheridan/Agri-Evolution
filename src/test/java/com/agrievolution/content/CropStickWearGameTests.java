package com.agrievolution.content;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class CropStickWearGameTests {
    @GameTest(template="empty") public static void lifetimesAndRecovery(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,40,3));
        level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        for(int tier=1;tier<=2;tier++){
            var state=AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,tier);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,state,2);
            var plant=(AECropBlockEntity)level.getBlockEntity(pos);
            h.assertTrue(plant.supportTicks()==(tier==1?24000:288000),"Correct full lifetime");
            AECropBlockEntity.tickSupport(level,pos,state,plant);
            var recovered=plant.recoverSupport();h.assertTrue(CropSupportItem.remaining(recovered)==CropSupportItem.lifetime(tier)-1,"Recovered item retains wear");
            var tag=new net.minecraft.nbt.CompoundTag();plant.saveAdditional(tag,level.registryAccess());
            var copy=new AECropBlockEntity(pos,state);copy.loadAdditional(tag,level.registryAccess());h.assertTrue(copy.supportTicks()==plant.supportTicks(),"Wear survives save/load");
            plant.setSupportTicks(1);AECropBlockEntity.tickSupport(level,pos,state,plant);
            h.assertTrue(CropSupportItem.tier(level.getBlockState(pos))==0&&level.getBlockEntity(pos)==plant,"Expiry removes sticks only");
        }h.succeed();
    }
    @GameTest(template="empty") public static void obsidianAndWeedProtection(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,40,3));level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);
        var state=AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED,true).setValue(AgriCropBlock.SUPPORT_TIER,3).setValue(AgriCropBlock.WEEDY,true);
        level.setBlock(pos,state,2);var plant=(AECropBlockEntity)level.getBlockEntity(pos);
        for(int i=0;i<25000;i++)AECropBlockEntity.tickSupport(level,pos,level.getBlockState(pos),plant);
        h.assertTrue(CropSupportItem.tier(level.getBlockState(pos))==3&&!level.getBlockState(pos).getValue(AgriCropBlock.WEEDY),"Obsidian persists and clears weeds");
        h.assertTrue(CropSupportItem.weedChance(1)==80&&CropSupportItem.weedChance(2)==800&&CropSupportItem.weedChance(3)==0,"Iron has tenth the weed probability; obsidian disables weeds");h.succeed();
    }
}
