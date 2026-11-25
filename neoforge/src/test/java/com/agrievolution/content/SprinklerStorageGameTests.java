package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class SprinklerStorageGameTests {
    private static IrrigationSprinklerBlockEntity head(GameTestHelper h,BlockPos pos){
        h.getLevel().setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        return (IrrigationSprinklerBlockEntity)h.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void removedSprinklerHandlesCannotTransferWater(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 155, 3));
        var old = head(h, pos);
        h.assertTrue(old.fillBucket(), "The original sprinkler can accept a bucket");
        level.removeBlock(pos, false);
        var replacement = head(h, pos);
        h.assertTrue(old.isRemoved() && replacement != old && !old.fillBucket() && !old.takeBucket(),
                "The detached sprinkler rejects both filling and draining even after replacement");
        h.assertTrue(old.water() == 1000 && replacement.water() == 0 && replacement.fillBucket() && replacement.takeBucket(),
                "The replacement owns an independent, usable tank");
        h.succeed();
    }
    @GameTest(template="empty") public static void fourBucketCapacityAndPersistence(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,120,3));var head=head(h,pos);
        for(int i=0;i<4;i++)h.assertTrue(head.fillBucket(),"Four buckets accepted");
        h.assertTrue(head.water()==4000,"Exactly 4000 mB stored");
        h.assertFalse(head.fillBucket(),"Fifth bucket rejected without overflow");
        var tag=new CompoundTag();head.saveAdditional(tag,h.getLevel().registryAccess());
        var copy=new IrrigationSprinklerBlockEntity(pos,h.getLevel().getBlockState(pos));copy.loadAdditional(tag,h.getLevel().registryAccess());
        h.assertTrue(copy.water()==4000,"All four stored buckets persist");
        tag.putInt("SprinklerWater",975);copy.loadAdditional(tag,h.getLevel().registryAccess());
        h.assertTrue(copy.water()==975,"Legacy stored water remains usable without its old source");h.succeed();
    }

    @GameTest(template = "empty")
    public static void offhandBucketsTakePriorityOverInspection(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 175, 3));
        var sprinkler = head(h, pos);
        var block = AEBlocks.IRRIGATION_SPRINKLER.get();
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        for (var bucket : new Item[]{Items.WATER_BUCKET, Items.BUCKET}) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(bucket));
            h.assertTrue(block.useItemOn(ItemStack.EMPTY, level.getBlockState(pos), level, pos, player,
                            InteractionHand.MAIN_HAND, hit) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "The empty main hand leaves the click to the offhand bucket");
            block.useItemOn(player.getOffhandItem(), level.getBlockState(pos), level, pos, player,
                    InteractionHand.OFF_HAND, hit);
            boolean filling = bucket == Items.WATER_BUCKET;
            h.assertTrue(sprinkler.water() == (filling ? 1000 : 0)
                            && player.getOffhandItem().is(filling ? Items.BUCKET : Items.WATER_BUCKET),
                    "The transfer conserves water and updates the offhand");
        }
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        h.assertTrue(block.useItemOn(ItemStack.EMPTY, level.getBlockState(pos), level, pos, player,
                        InteractionHand.MAIN_HAND, hit) == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "A normal empty-hand click still reaches inspection");
        h.succeed();
    }
    @GameTest(template="empty") public static void manualBucketWorksWithOrphanPipes(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,130,3));var head=head(h,pos);
        level.setBlock(pos.east(),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        h.assertTrue(IrrigationNetwork.search(level,pos).attached()&&IrrigationNetwork.search(level,pos).supplies().isEmpty(),"Fixture has pipes but no reservoir");
        head.fillBucket();
        for(int i=0;i<40;i++){head.irrigateCycle();h.assertTrue(head.growthActive(),"Paid manual water continues without reservoir");}
        h.assertTrue(head.water()==0,"Forty cycles consume exactly one bucket");
        head.irrigateCycle();h.assertFalse(head.growthActive(),"Empty manual sprinkler stops");h.succeed();
    }
    @GameTest(template="empty") public static void remoteValveStillPausesManualWater(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,140,3));var head=head(h,pos);
        level.setBlock(pos.east(),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        var valve=pos.east(2);level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN,false),3);
        head.fillBucket();head.irrigateCycle();
        h.assertTrue(head.water()==1000&&!head.growthActive(),"Closed valve preserves manual water");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,true),3);head.irrigateCycle();
        h.assertTrue(head.water()==975&&head.growthActive(),"Open valve runs manual water with no source");
        level.setBlock(valve.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Powered remote valve pauses manual water");h.succeed();
    }
    @GameTest(template="empty") public static void fullTankRejectsBucketAndTopupConserves(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,150,3));var head=head(h,pos);
        for(int i=0;i<4;i++)head.fillBucket();
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        AEBlocks.IRRIGATION_SPRINKLER.get().useItemOn(player.getMainHandItem(),level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(head.water()==4000&&player.getMainHandItem().is(Items.WATER_BUCKET),"Full tank leaves bucket untouched");
        for(int i=0;i<40;i++)head.irrigateCycle();
        AEBlocks.IRRIGATION_SPRINKLER.get().useItemOn(player.getMainHandItem(),level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(head.water()==4000&&player.getMainHandItem().is(Items.BUCKET),"Top-up restores 4000 without extra watering pulse");h.succeed();
    }
}
