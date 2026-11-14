package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ReservoirGameTests {
    static WaterReservoirBlockEntity tank(GameTestHelper h,BlockPos core){
        blueprint(h,core);h.assertTrue(ReservoirStructure.tryForm(h.getLevel(),core.north()),"Valid shell must form");
        return (WaterReservoirBlockEntity)h.getLevel().getBlockEntity(core);
    }
    static void blueprint(GameTestHelper h,BlockPos core){
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            h.getLevel().setBlock(core.offset(x,y,z),(x==0&&y==0&&z==0?Blocks.WATER:Blocks.COPPER_BLOCK).defaultBlockState(),2);
    }
    @GameTest(template="empty") public static void formationStorageAndDismantle(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,20,3));var tank=tank(h,core);
        h.assertTrue(tank.water()==1000,"Source must become exactly one stored bucket");
        for(int i=1;i<32;i++)h.assertTrue(tank.addBucket(),"Tank should accept remaining capacity");
        h.assertFalse(tank.addBucket(),"Full tank must reject bucket");
        var tag=new CompoundTag();tank.saveAdditional(tag,level.registryAccess());
        var restored=new WaterReservoirBlockEntity(core,level.getBlockState(core));restored.loadAdditional(tag,level.registryAccess());
        h.assertTrue(restored.water()==32000,"Tank must persist water");
        level.setBlock(core.north(),Blocks.AIR.defaultBlockState(),3);
        int copper=0;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockState(core.offset(x,y,z)).is(Blocks.COPPER_BLOCK))copper++;
        h.assertTrue(copper==25,"Breaking one shell must restore the other 25 copper blocks");
        h.assertTrue(level.getBlockState(core).is(Blocks.WATER),"Nonempty dismantled tank may return only its original source");
        h.succeed();
    }
    @GameTest(template="empty") public static void invalidBlueprintAndEmptyDismantle(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,30,3));blueprint(h,core);
        level.setBlock(core.above(),Blocks.AIR.defaultBlockState(),2);
        h.assertFalse(ReservoirStructure.tryForm(level,core.north()),"Incomplete shell must not form");
        h.assertTrue(level.getBlockState(core).is(Blocks.WATER),"Failed formation must preserve water");
        level.setBlock(core.above(),Blocks.COPPER_BLOCK.defaultBlockState(),2);
        h.assertTrue(ReservoirStructure.tryForm(level,core.north()),"Repaired shell forms");
        var tank=(WaterReservoirBlockEntity)level.getBlockEntity(core);h.assertTrue(tank.takeBucket(),"Drain original water");
        level.setBlock(core.north(),Blocks.AIR.defaultBlockState(),3);
        h.assertTrue(level.getBlockState(core).isAir(),"Empty tank must not recreate its original water source");h.succeed();
    }
    @GameTest(template="empty") public static void remoteValveAndPipeBreak(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,40,3));var tank=tank(h,core);
        for(int i=2;i<=5;i++)level.setBlock(core.east(i),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        var valve=core.east(3);level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.EAST),3);
        var pos=core.east(6);level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);head.irrigateCycle();
        h.assertTrue(tank.water()==0&&head.water()==975,"Remote transfer must debit one tank bucket");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Remote closed valve must stop buffered sprinkler");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,true),3);head.irrigateCycle();
        h.assertTrue(head.growthActive(),"Empty but intact source permits paid buffer use");
        level.setBlock(valve.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Remote powered valve must stop boost");
        level.setBlock(valve.above(),Blocks.AIR.defaultBlockState(),3);level.setBlock(core.east(4),Blocks.AIR.defaultBlockState(),3);head.checkFlow();
        head.irrigateCycle();h.assertTrue(head.growthActive(),"Disconnected supply cannot disable already-paid stored water");h.succeed();
    }
    @GameTest(template="empty") public static void branchesShareFiniteWater(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,50,3));var tank=tank(h,core);
        var junction=core.east(2);level.setBlock(junction,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        for(var pos:new BlockPos[]{junction.north(),junction.south()})level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var a=(IrrigationSprinklerBlockEntity)level.getBlockEntity(junction.north());var b=(IrrigationSprinklerBlockEntity)level.getBlockEntity(junction.south());
        a.irrigateCycle();b.irrigateCycle();h.assertTrue(a.water()+b.water()==975&&tank.water()==0,"Branches must not duplicate one source bucket");
        tank.addBucket();b.irrigateCycle();h.assertTrue(b.water()==975&&tank.water()==0,"Refill supplies waiting branch");h.succeed();
    }
    @GameTest(template="empty") public static void bucketStacksAndDropConservation(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,60,3));var tank=tank(h,core);
        var pos=core.north();var state=level.getBlockState(pos);var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET,2));
        AEBlocks.WATER_RESERVOIR.get().useItemOn(player.getMainHandItem(),state,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        h.assertTrue(tank.water()==0&&player.getMainHandItem().getCount()==1,"Draining must consume just one empty bucket");
        int filled=player.getInventory().countItem(net.minecraft.world.item.Items.WATER_BUCKET);
        h.assertTrue(filled==1,"Draining a stack must return exactly one filled bucket");
        var drops=net.minecraft.world.level.block.Block.getDrops(state,level,pos,null);
        h.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.world.item.Items.COPPER_BLOCK)&&drops.getFirst().getCount()==1,"Broken shell must drop one original copper block");
        var centerDrops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(core),level,core,tank);
        h.assertTrue(centerDrops.isEmpty(),"Water-source centre must never drop extra copper");h.succeed();
    }
    @GameTest(template="empty") public static void pipeCycleAndClosedAlternateBranch(GameTestHelper h){
        var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,70,3));var tank=tank(h,core);
        // A four-pipe loop with one reachable reservoir and an unrelated closed branch.
        for(var p:new BlockPos[]{core.east(2),core.east(3),core.east(2).south(),core.east(3).south()})level.setBlock(p,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
        level.setBlock(core.east(2).north(),AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN,false),3);
        var pos=core.east(4);level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var search=IrrigationNetwork.search(level,pos);
        h.assertTrue(!search.overflow()&&search.supplies().size()==1,"Loop terminates and deduplicates tank source");
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);head.irrigateCycle();
        h.assertTrue(head.water()==975&&tank.water()==0,"Unrelated closed branch must not shut off an open supply path");h.succeed();
    }
    @GameTest(template="empty") public static void oversizedNetworkFailsClosed(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(1,90,1));
        for(int x=0;x<9;x++)for(int y=0;y<7;y++)for(int z=0;z<9;z++)
            level.setBlock(origin.offset(x,y,z),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),2);
        var head=origin.west();level.setBlock(head,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var search=IrrigationNetwork.search(level,head);
        h.assertTrue(search.overflow()&&search.supplies().isEmpty(),"Oversized network must stop at the traversal limit without exposing partial supply");h.succeed();
    }
}
