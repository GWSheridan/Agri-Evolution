package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationGameTests {
    private static BlockPos position(GameTestHelper h) { return h.absolutePos(new BlockPos(4,2,4)); }
    private static IrrigationSprinklerBlockEntity setup(GameTestHelper h, boolean basin) {
        var level=h.getLevel(); var pos=position(h);
        level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        if(basin) {
            level.setBlock(pos.west(), AEBlocks.IRRIGATION_VALVE.get().defaultBlockState()
                    .setValue(IrrigationValveBlock.FACING,Direction.EAST),3);
            level.setBlock(pos.west(2),AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                    .setValue(IrrigationBasinBlock.FILLED,true),3);
        }
        return (IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);
    }
    @GameTest(template="empty")
    public static void transfersWaterWithoutDuplication(GameTestHelper h) {
        var sprinkler=setup(h,true); var level=h.getLevel(); var pos=position(h);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==975,"One bucket transferred minus one 25 mB interval");
        h.assertFalse(level.getBlockState(pos.west(2)).getValue(IrrigationBasinBlock.FILLED),"Basin must be emptied");
        for(int i=0;i<39;i++) sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==0,"One bucket must last exactly 40 intervals");
        sprinkler.irrigateCycle();
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE),"Empty sprinkler must stop");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void closedAndPoweredValvesPause(GameTestHelper h) {
        var sprinkler=setup(h,true); var level=h.getLevel(); var pos=position(h); var valve=pos.west();
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==0 && level.getBlockState(pos.west(2)).getValue(IrrigationBasinBlock.FILLED),"Closed valve cannot draw water");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,true),3);
        sprinkler.irrigateCycle();
        level.setBlock(valve.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        sprinkler.checkFlow(); sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==975,"Powered valve preserves stored water");
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE),"Powered valve stops irrigation");
        level.setBlock(valve.above(),Blocks.AIR.defaultBlockState(),3);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==950,"Unpowered open valve resumes");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void reservoirPersistsAndClamps(GameTestHelper h) {
        var sprinkler=setup(h,false); var level=h.getLevel(); var pos=position(h);
        sprinkler.fillBucket(); sprinkler.irrigateCycle();
        var tag=new CompoundTag(); sprinkler.saveAdditional(tag,level.registryAccess());
        var restored=new IrrigationSprinklerBlockEntity(pos,level.getBlockState(pos));
        restored.loadAdditional(tag,level.registryAccess());
        h.assertTrue(restored.water()==975,"Reservoir must survive save/reload");
        tag.putInt("SprinklerWater",-200); restored.loadAdditional(tag,level.registryAccess());
        h.assertTrue(restored.water()==0,"Negative water must clamp");
        tag.putInt("SprinklerWater",20000); restored.loadAdditional(tag,level.registryAccess());
        h.assertTrue(restored.water()==4000,"Over-capacity water must clamp");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void bucketInteractionConservesItems(GameTestHelper h) {
        var sprinkler=setup(h,false); var level=h.getLevel(); var pos=position(h);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        var block=AEBlocks.IRRIGATION_SPRINKLER.get();
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        block.useItemOn(player.getMainHandItem(),level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET),"Water bucket must return empty bucket");
        h.assertTrue(sprinkler.water()==975,"Bucket fills and starts one interval");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        block.useItemOn(player.getMainHandItem(),level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && sprinkler.water()==1975,"Top-up adds a bucket without forcing another watering interval");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void valveOrientationAndManualToggle(GameTestHelper h) {
        var sprinkler=setup(h,true); var level=h.getLevel(); var pos=position(h); var valve=pos.west();
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.FACING,Direction.WEST),3);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water()==975,"Either end of an aligned valve must draw water");
        level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.FACING,Direction.EAST),3);
        sprinkler.irrigateCycle();
        AEBlocks.IRRIGATION_VALVE.get().useWithoutItem(level.getBlockState(valve),level,valve,
                h.makeMockPlayer(GameType.SURVIVAL),new BlockHitResult(Vec3.atCenterOf(valve),Direction.UP,valve,false));
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE),"Manual closure stops immediately");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void verticalValveAndHeldItemControl(GameTestHelper h) {
        var sprinkler=setup(h,false); var level=h.getLevel(); var pos=position(h); var valve=pos.above();
        level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.DOWN),3);
        h.assertTrue(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(Direction.UP)),"Vertical connection must be visible");
        sprinkler.fillBucket(); sprinkler.irrigateCycle();
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AEBlocks.IRRIGATION_SPRINKLER.get()));
        var hit=new BlockHitResult(Vec3.atCenterOf(valve),Direction.NORTH,valve,false);
        AEBlocks.IRRIGATION_VALVE.get().useItemOn(player.getMainHandItem(),level.getBlockState(valve),level,valve,player,InteractionHand.MAIN_HAND,hit);
        h.assertFalse(level.getBlockState(valve).getValue(IrrigationValveBlock.OPEN),"Holding a block must still close the valve");
        h.assertFalse(sprinkler.growthActive(),"Manual close must stop x5 immediately");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Toggle must not consume held block");
        AEBlocks.IRRIGATION_VALVE.get().useItemOn(player.getMainHandItem(),level.getBlockState(valve),level,valve,player,InteractionHand.MAIN_HAND,hit);
        sprinkler.irrigateCycle();
        level.setBlock(valve.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        h.assertTrue(level.getBlockState(valve).getValue(IrrigationValveBlock.POWERED),"Vertical valve must reflect external redstone power");
        h.assertFalse(sprinkler.growthActive(),"Redstone must stop vertical head growth immediately");
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE),"Redstone must stop spray immediately");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void allSixValveConnections(GameTestHelper h) {
        var sprinkler=setup(h,false); var level=h.getLevel(); var pos=position(h);
        for(var side:Direction.values()) {
            var valve=pos.relative(side); var basin=pos.relative(side,2);
            level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING,side),3);
            level.setBlock(basin,AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED,true),3);
            h.assertTrue(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(side)),"Connector missing: "+side);
            sprinkler.irrigateCycle();
            h.assertTrue(sprinkler.water()==975,"Supply must work along "+side);
            h.assertFalse(level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED),"Source must be consumed: "+side);
            for(int i=0;i<39;i++) sprinkler.irrigateCycle();
            level.setBlock(valve,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(basin,Blocks.AIR.defaultBlockState(),3);
            h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(side)),"Removed connector must disappear");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void valvePlacedFirstAutoAligns(GameTestHelper h) {
        var level=h.getLevel(); var pos=position(h); var valve=pos.above();
        level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(),3);
        var sprinkler=setup(h,false);
        h.assertTrue(level.getBlockState(valve).getValue(IrrigationValveBlock.FACING).getAxis()==Direction.Axis.Y,"Adding head below must align old horizontal valve");
        h.assertTrue(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(Direction.UP)),"Auto-aligned connection must render");
        sprinkler.fillBucket(); sprinkler.irrigateCycle();
        level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        h.assertFalse(sprinkler.growthActive(),"Powering head directly must stop boost");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void hydratesVanillaAndAdvancedWithinRange(GameTestHelper h) {
        var sprinkler=setup(h,false); var level=h.getLevel(); var center=position(h).below();
        var inside=center.east(4); var advanced=center.south(4); var outside=center.east(5);
        level.setBlock(inside,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,0),3);
        level.setBlock(advanced,AEBlocks.CELESTIAL_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,0),3);
        level.setBlock(outside,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,0),3);
        sprinkler.fillBucket(); sprinkler.irrigateCycle();
        h.assertTrue(level.getBlockState(inside).getValue(FarmBlock.MOISTURE)==7,"Edge vanilla farmland must hydrate");
        h.assertTrue(level.getBlockState(advanced).getValue(FarmBlock.MOISTURE)==7,"Advanced farmland must hydrate");
        h.assertTrue(level.getBlockState(outside).getValue(FarmBlock.MOISTURE)==0,"Outside radius must remain dry");
        h.succeed();
    }
}
