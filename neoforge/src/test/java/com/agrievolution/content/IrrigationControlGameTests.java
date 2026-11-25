package com.agrievolution.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class IrrigationControlGameTests {
 @GameTest(template = "empty")
 public static void sourceReportsKeepPartialTanksSeparate(GameTestHelper h) {
  var level = h.getLevel();
  var pos = h.absolutePos(new BlockPos(4, 115, 4));
  var nearCore = pos.east(4);
  var farCore = pos.south(5);
  var near = ReservoirGameTests.tank(h, nearCore);
  var far = ReservoirGameTests.tank(h, farCore);
  near.drainWater(625, true);
  far.drainWater(175, true);
  var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
  for (int i = 1; i <= 2; i++) level.setBlock(pos.east(i), pipe, Block.UPDATE_ALL);
  for (int i = 1; i <= 3; i++) level.setBlock(pos.south(i), pipe, Block.UPDATE_ALL);
  level.setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);
  var head = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pos);
  var route = IrrigationNetwork.trace(level, pos);
  h.assertTrue(route.flow() == IrrigationNetwork.Flow.EMPTY && route.blocks().getLast().equals(farCore.north())
          && route.sourceWater() == 825 && SpannerItem.routeStatus(level, route).contains("825 mB"),
          "The trace points to the fullest partial tank even when it is farther away");
  h.assertTrue(FarmInspection.describe(level, pos, true, false).contains("Sources ready: 0/2 | 1200 mB total")
          && head.inspectionStatus().contains("fullest: 825 mB"), "Combined water does not count as a deliverable bucket");
  h.assertTrue(near.water() == 375 && far.water() == 825 && head.water() == 0, "Inspection moves no water");

  level.setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
  far.fillWater(175, true);
  route = IrrigationNetwork.trace(level, pos);
  h.assertTrue(route.flow() == IrrigationNetwork.Flow.READY && route.blocks().getLast().equals(farCore.north())
          && route.sourceWater() == 1000 && SpannerItem.routeStatus(level, route).contains("1000 mB"),
          "A tank with exactly one bucket is ready");
  String zone = SpannerItem.describeZone(level, pos, level.getBlockState(pos), "Orchard").getFirst();
  h.assertTrue(zone.contains("Supply water: 1375 / 64000 mB") && zone.contains("Sources ready: 1/2")
          && far.water() == 1000, "The valve report distinguishes ready sources from total storage");
  h.succeed();
 }

 @GameTest(template="empty")
 public static void namedZoneReadsWaterWithoutOpeningValve(GameTestHelper helper) {
  var level = helper.getLevel();
  var core = helper.absolutePos(new BlockPos(3, 100, 3));
  var tank = ReservoirGameTests.tank(helper, core);
  var pos = core.east(2);
  var head = pos.east();
  level.setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN, false), 3);
  level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), 3);
  var valve = (IrrigationValveBlockEntity) level.getBlockEntity(pos);
  valve.rename(" North garden ");
  var tag = new net.minecraft.nbt.CompoundTag();
  valve.saveAdditional(tag, level.registryAccess());
  var restored = new IrrigationValveBlockEntity(pos, level.getBlockState(pos));
  restored.loadAdditional(tag, level.registryAccess());
  var status = IrrigationNetwork.inspectZone(level, pos);
  helper.assertTrue(restored.zone().equals("North garden") && status.sprinklers().equals(java.util.List.of(head))
          && status.supplies().getFirst().reservoir() == tank && tank.water() == 1000
          && IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.CLOSED_VALVE,
          "Zone names persist and inspection can see supply and heads around a closed valve without moving water");
  var overlay = FarmInspection.describe(level, pos, true, false);
  helper.assertTrue(overlay.contains("Zone: North garden") && overlay.contains("Valve closed")
          && overlay.contains("Zone sources ready: 1/1 | 1000 mB total")
          && SpannerItem.describeZone(level, pos, level.getBlockState(pos), valve.zone()).getFirst().contains("Sources ready: 1/1")
          && tank.water() == 1000 && !level.getBlockState(pos).getValue(IrrigationValveBlock.OPEN),
          "The overlay and right-click report describe the same closed valve and stored water");
  helper.succeed();
 }

 @GameTest(template = "empty")
 public static void namedValveMovesWithoutLoadingTheNextChunk(GameTestHelper h) {
  var level = h.getLevel();
  var chunk = new ChunkPos(h.absolutePos(new BlockPos(4352, 100, 4)));
  var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
  var pos = edge.west();
  var destination = pos.west(3);
  int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
  level.getChunkAt(pos);
  level.setBlock(edge, Blocks.STONE.defaultBlockState(), quiet);
  level.setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.OPEN, false), quiet);
  var valve = (IrrigationValveBlockEntity) level.getBlockEntity(pos);
  var unnamed = Block.getDrops(level.getBlockState(pos), level, pos, valve).getFirst();
  h.assertTrue(ItemStack.isSameItemSameComponents(unnamed, new ItemStack(AEItems.IRRIGATION_VALVE.get())),
          "An unnamed valve drop still stacks with a newly crafted valve");
  h.assertFalse(level.hasChunkAt(edge.east()), "The neighboring chunk starts unloaded");
  valve.rename("  Orchard  ");
  h.assertTrue(!level.hasChunkAt(edge.east()) && FarmInspection.describe(level, pos, true, false).contains("Zone: Orchard"),
          "Renaming avoids comparator lookups across the border, and the HUD displays the zone name");
  var drop = Block.getDrops(level.getBlockState(pos), level, pos, valve).getFirst();
  h.assertTrue(IrrigationValveBlockEntity.storedZone(drop).equals("Orchard"), "The valve drop keeps its saved name");
  level.setBlock(pos, Blocks.AIR.defaultBlockState(), quiet);
  level.setBlock(destination.below(), Blocks.STONE.defaultBlockState(), quiet);
  var player = h.makeMockPlayer(GameType.SURVIVAL);
  player.setItemInHand(InteractionHand.MAIN_HAND, drop);
  var hit = new BlockHitResult(Vec3.atCenterOf(destination.below()), Direction.UP, destination.below(), false);
  h.assertTrue(drop.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Place the dropped valve item");
  h.assertTrue(level.getBlockEntity(destination) instanceof IrrigationValveBlockEntity placed && placed.zone().equals("Orchard")
          && level.getBlockState(destination).getValue(IrrigationValveBlock.OPEN), "The relocated valve keeps its name and starts open");
  h.succeed();
 }

 @GameTest(template="empty")
 public static void valveReportIncludesBasinsAndChecksWhetherHeadsAreSpraying(GameTestHelper h) {
  var level = h.getLevel();
  var core = h.absolutePos(new BlockPos(3, 110, 3));
  var tank = ReservoirGameTests.tank(h, core);
  var valve = core.east(2);
  var basin = valve.south();
  var head = valve.east();
  level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
  level.setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), Block.UPDATE_ALL);
  level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);
  // A saved activity flag without a current spray interval must not count as active.
  level.setBlock(head, level.getBlockState(head).setValue(IrrigationSprinklerBlock.ACTIVE, true), Block.UPDATE_ALL);
  var report = SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden");
  h.assertTrue(report.getFirst().contains("Supply water: 2000 / 33000 mB"), "The total includes the basin and the reservoir");
  h.assertTrue(report.getFirst().contains("Sprinklers: 0/1 active"), "The activity flag alone must not count as watering");
  h.assertTrue(tank.water() == 1000 && level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "Reading the zone must not move water");

  var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
  sprinkler.fillBucket();
  sprinkler.irrigateCycle();
  level.setBlock(basin, level.getBlockState(basin).setValue(IrrigationBasinBlock.FILLED, false), Block.UPDATE_ALL);
  var watering = SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden");
  h.assertTrue(watering.getFirst().contains("Supply water: 1000 / 33000 mB"), "An empty basin adds capacity and no water");
  h.assertTrue(watering.getFirst().contains("Sprinklers: 1/1 active"), "A current spray counts as active");
  var stopped = valve.above();
  level.setBlock(stopped, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);
  ((IrrigationSprinklerBlockEntity) level.getBlockEntity(stopped)).configure(true);
  var mixed = SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden");
  h.assertTrue(mixed.getFirst().contains("Sprinklers: 1/2 active")
          && mixed.get(1).contains(stopped.toShortString()) && mixed.get(1).contains("Waiting for redstone"),
          "The report identifies the stopped sprinkler even while another one runs");
  h.succeed();
 }

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
  ((IrrigationValveBlockEntity) level.getBlockEntity(valve)).rename("Back Beds");
  level.setBlock(valve.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
  h.assertTrue(head.inspectionStatus().equals("Supply valve paused by redstone (Back Beds) [" + valve.toShortString() + "]"),
          "Remote valve reports the zone, location and redstone shutoff");
  level.removeBlock(valve.above(), false);
  level.setBlock(valve,level.getBlockState(valve).setValue(IrrigationValveBlock.OPEN,false),3);
  h.assertTrue(head.inspectionStatus().equals("Supply valve closed (Back Beds) [" + valve.toShortString() + "]"),
          "Manual closure differs from redstone shutoff");
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
  var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
  h.assertTrue(sprinkler.inspectionStatus().equals("Refill blocked by disabled west pipe port"),
          "The sprinkler explains which pipe face needs opening");
  IrrigationPipeBlock.toggle(level,pipe,Direction.WEST);
  h.assertTrue(IrrigationNetwork.search(level,head).supplies().size()==1,"Reenabled port reconnects");h.succeed();
 }
}
