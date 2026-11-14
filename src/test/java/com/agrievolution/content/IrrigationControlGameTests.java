package com.agrievolution.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class IrrigationControlGameTests {
 @GameTest(template="empty") public static void drySoilModeAndPowerModes(GameTestHelper h){
  var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,20,4));
  level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
  var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);head.fillBucket();head.configure(false);
  h.assertTrue(head.inspectionStatus().contains("no farmland"),"Dry-soil status distinguishes missing farmland");
  head.irrigateCycle();h.assertTrue(head.water()==1000,"Smart mode must idle with no farmland");
  level.setBlock(pos.below(2),Blocks.FARMLAND.defaultBlockState(),3);head.irrigateCycle();
  h.assertTrue(head.water()==975,"Dry soil needs one watering interval");head.irrigateCycle();
  h.assertTrue(head.water()==975&&!head.growthActive(),"Wet soil consumes nothing and grants no idle boost");
  h.assertTrue(head.inspectionStatus().contains("soil already wet"),"Dry-soil status distinguishes wet farmland");
  head.configure(false);head.configure(true);head.irrigateCycle();h.assertTrue(head.water()==975,"Requires signal mode must wait");
  h.assertTrue(head.inspectionStatus().equals("Waiting for redstone"),"Required power has a distinct status");
  level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);head.irrigateCycle();h.assertTrue(head.water()==950,"Signal enables mode one");
  head.configure(true);level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),3);head.irrigateCycle();h.assertTrue(head.water()==925,"Ignore mode works without signal");h.succeed();
 }
 @GameTest(template="empty") public static void inspectionDistinguishesSupplyAndValveWithoutDraining(GameTestHelper h){
  var level=h.getLevel();
  var core=h.absolutePos(new BlockPos(3,60,3));
  var tank=ReservoirGameTests.tank(h,core);
  var valve=core.east(2);var pipe=core.east(3);var pos=core.east(4);
  level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN,true),3);
  level.setBlock(pipe,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);
  level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
  var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);
  h.assertTrue(head.inspectionStatus().equals("Connected source ready to refill"),"Full bucket ready");
  h.assertTrue(tank.water()==1000&&head.water()==0,"Inspection leaves both tanks intact");
  h.assertTrue(tank.takeBucket(),"Drain reservoir for the next check");
  h.assertTrue(head.inspectionStatus().equals("Connected sources short of a full bucket"),"Empty source is still connected");
  level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);
  h.assertTrue(head.inspectionStatus().equals("Valve shut along pipe network"),"Remote shut valve is reported");
  level.setBlock(pipe,Blocks.AIR.defaultBlockState(),3);
  h.assertTrue(head.inspectionStatus().equals("Empty, no reachable water source"),"Disconnected head differs from a dry source");
  level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
  h.assertTrue(head.inspectionStatus().equals("Paused by redstone"),"Default mode reports powered pause");
  h.succeed();
 }
 @GameTest(template="empty") public static void disabledPortSeparatesNetwork(GameTestHelper h){
  var level=h.getLevel();var core=h.absolutePos(new BlockPos(3,40,3));ReservoirGameTests.tank(h,core);
  var pipe=core.east(2);var head=pipe.east();level.setBlock(pipe,AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),3);level.setBlock(head,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
  h.assertTrue(IrrigationNetwork.search(level,head).supplies().size()==1,"Initial network connected");
  IrrigationPipeBlock.toggle(level,pipe,Direction.WEST);
  h.assertTrue(IrrigationNetwork.search(level,head).supplies().isEmpty(),"Disabled source port isolates zone");
  h.assertFalse(level.getBlockState(pipe).getValue(IrrigationSprinklerBlock.connection(Direction.WEST)),"Disabled connector disappears visually");
  IrrigationPipeBlock.toggle(level,pipe,Direction.WEST);
  h.assertTrue(IrrigationNetwork.search(level,head).supplies().size()==1,"Reenabled port reconnects");h.succeed();
 }
}
