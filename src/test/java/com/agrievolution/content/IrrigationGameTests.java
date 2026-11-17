package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.FarmlandWaterManager;
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
    public static void traceShowsSprinklerShutoffWithAlternateSupply(GameTestHelper h) {
        var sprinkler = setup(h, false);
        var level = h.getLevel();
        var pos = position(h);
        var valve = pos.above();
        var pipe = pos.east();
        var basin = pipe.east();
        var openValve = AEBlocks.IRRIGATION_VALVE.get().defaultBlockState();
        var closedValve = openValve.setValue(IrrigationValveBlock.OPEN, false);
        level.setBlock(valve, closedValve, 3);
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        level.setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), 3);
        var shutoffRoute = new IrrigationNetwork.Route(List.of(pos, valve), IrrigationNetwork.Flow.CLOSED_VALVE, null);
        var supplyRoute = new IrrigationNetwork.Route(List.of(pos, pipe, basin), IrrigationNetwork.Flow.READY, null, null, 1000);

        sprinkler.fillBucket();
        sprinkler.irrigateCycle();
        h.assertTrue(!sprinkler.growthActive() && sprinkler.water() == 1000, "An adjacent shutoff preserves buffered water");
        h.assertTrue(IrrigationNetwork.trace(level, pos).equals(shutoffRoute), "Trace should highlight the adjacent closed valve");

        level.setBlock(valve, openValve, 3);
        h.assertTrue(IrrigationNetwork.trace(level, pos).equals(supplyRoute), "An open valve lets the trace find the full basin");
        level.setBlock(valve.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        h.assertTrue(IrrigationNetwork.trace(level, pos).equals(shutoffRoute), "Trace should also highlight a powered shutoff valve");
        h.assertTrue(level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "Tracing must preserve the basin's water");

        level.setBlock(valve.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(valve, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pipe.above(), closedValve, 3);
        h.assertTrue(IrrigationNetwork.trace(level, pos).equals(supplyRoute), "The trace can bypass a closed valve off the supply route");
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.growthActive(), "A closed valve on a different branch leaves the sprinkler running");

        level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        var paused = IrrigationNetwork.trace(level, pos);
        h.assertTrue(paused.flow() == IrrigationNetwork.Flow.PAUSED_BY_REDSTONE && paused.blocks().equals(List.of(pos))
                        && SpannerItem.routeStatus(level, paused).equals("Sprinkler paused by redstone"),
                "The trace identifies the powered head even with a full basin available");
        sprinkler.configure(true);
        level.setBlock(pos.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        var waiting = IrrigationNetwork.trace(level, pos);
        h.assertTrue(waiting.flow() == IrrigationNetwork.Flow.WAITING_FOR_REDSTONE && waiting.blocks().equals(List.of(pos)),
                "A head set to require power reports the missing signal");
        sprinkler.configure(true);
        level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(IrrigationNetwork.trace(level, pos).equals(supplyRoute)
                        && level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED),
                "Ignore-redstone mode traces the supply without draining it");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void sprinklerComparatorTracksWaterAndRemoval(GameTestHelper h) {
        var sprinkler = setup(h, false);
        sprinkler.configure(false);
        var level = h.getLevel();
        var pos = position(h);
        var direct = pos.south();
        var through = pos.west(2);
        level.setBlock(direct.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(through.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        var comparator = Blocks.COMPARATOR.defaultBlockState();
        level.setBlock(direct, comparator.setValue(ComparatorBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        level.setBlock(through, comparator.setValue(ComparatorBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        h.assertTrue(level.getBlockState(pos).hasAnalogOutputSignal() && level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 0,
                "An empty sprinkler outputs zero");
        for (int bucket = 0; bucket < 4; bucket++) h.assertTrue(sprinkler.fillBucket(), "Tank takes four buckets");
        h.runAfterDelay(5, () -> {
            h.assertTrue(level.getBlockState(direct).getSignal(level, direct, Direction.NORTH) == 15
                    && level.getBlockState(through).getSignal(level, through, Direction.EAST) == 15,
                    "A full tank reads 15 directly and through a solid block");
            sprinkler.configure(false);
            sprinkler.irrigateCycle();
            h.assertTrue(sprinkler.water() == 3975 && sprinkler.growthActive(), "A reading comparator leaves the sprinkler enabled");
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(level.getBlockState(direct).getSignal(level, direct, Direction.NORTH) == 14
                    && level.getBlockState(through).getSignal(level, through, Direction.EAST) == 14,
                    "Both comparators update when the spray uses water");
            level.removeBlock(pos, false);
        });
        h.runAfterDelay(15, () -> {
            h.assertTrue(level.getBlockState(direct).getSignal(level, direct, Direction.NORTH) == 0
                    && level.getBlockState(through).getSignal(level, through, Direction.EAST) == 0,
                    "Removing the sprinkler clears both comparators");
            h.succeed();
        });
    }

    @GameTest(template="empty")
    public static void sprinklerComparatorUpdateLeavesNeighboringChunkUnloaded(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(1280, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var pos = edge.west();
        var outside = edge.east();
        level.getChunkAt(edge);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(edge, Blocks.STONE.defaultBlockState(), quietUpdate);
        var soil = pos.west().below(2);
        var pipe = pos.north();
        var south = IrrigationSprinklerBlock.connection(Direction.SOUTH);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), quietUpdate);
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState().setValue(south, true), quietUpdate);
        level.setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), quietUpdate);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pos);
        sprinkler.fillBucket();
        h.assertTrue(!level.hasChunkAt(outside) && level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 4,
                "Updating the tank must leave the chunk beyond the stone unloaded");
        sprinkler.startAfterFill();
        h.assertTrue(!level.hasChunkAt(outside) && sprinkler.water() == 975 && sprinkler.growthActive(),
                "Starting a spray must still leave the chunk beyond the stone unloaded");
        h.assertTrue(FarmlandWaterManager.hasBlockWaterTicket(level, soil), "The active head supplies the loaded soil");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), quietUpdate);
        h.assertTrue(sprinkler.isRemoved() && !FarmlandWaterManager.hasBlockWaterTicket(level, soil)
                        && !level.getBlockState(pipe).getValue(south) && !level.hasChunkAt(outside),
                "Removing a sprinkler clears its hydration and the pipe arm without probing beyond the stone");

        level.setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), quietUpdate);
        level.setBlock(pipe, level.getBlockState(pipe).setValue(south, true), quietUpdate);
        var valve = level.getBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), quietUpdate);
        h.assertTrue(valve != null && valve.isRemoved() && !level.getBlockState(pipe).getValue(south) && !level.hasChunkAt(outside),
                "Removing a valve also leaves the next chunk unloaded and clears the adjoining pipe arm");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void savedSprinklerRepairsBothFittingsAndRespectsDisabledPorts(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 123, 4));
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var west = IrrigationSprinklerBlock.connection(Direction.WEST);
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos.east(), pipe, quiet);
        level.setBlock(pos.west(), pipe, quiet);
        h.assertTrue(IrrigationPipeBlock.toggle(level, pos.west(), Direction.EAST), "Close the west pipe's face");
        level.setBlock(pos.west(), level.getBlockState(pos.west()).setValue(east, true), quiet);
        level.setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(west, true), quiet);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pos);

        sprinkler.onLoad();
        h.assertTrue(level.getBlockState(pos).getValue(east) && level.getBlockState(pos.east()).getValue(west),
                "Both ends of the open connection repair their arms");
        h.assertTrue(!level.getBlockState(pos).getValue(west) && !level.getBlockState(pos.west()).getValue(east)
                        && !IrrigationPipeBlock.enabled(level.getBlockState(pos.west()), Direction.EAST),
                "Neither end of a disabled pipe draws a connector, and the disabled face stays closed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sprinklerRepairsConnectionWhenAdjacentChunkBecomesAvailable(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(4096, 100, 4)));
        var head = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = head.east();
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var west = IrrigationSprinklerBlock.connection(Direction.WEST);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.getChunkAt(head);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(east, true), quiet);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        sprinkler.onLoad();
        h.assertTrue(!level.hasChunkAt(outside) && level.getBlockState(head).getValue(east),
                "A saved fitting keeps its unloaded face without forcing a chunk load");

        level.getChunkAt(outside);
        level.setBlock(outside, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quiet);
        h.runAfterDelay(40 - level.getGameTime() % 40, () -> {
            sprinkler.serverTick();
            h.assertTrue(level.getBlockState(head).getValue(east) && level.getBlockState(outside).getValue(west),
                    "The next fitting check repairs the pipe that arrived after the head");
            h.assertTrue(sprinkler.water() == 0 && !sprinkler.growthActive(), "Updating the connector neither adds water nor starts a spray");
            h.succeed();
        });
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

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        block.useItemOn(player.getMainHandItem(), level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 1
                && player.getInventory().countItem(Items.WATER_BUCKET) == 1, "Take one empty bucket and return one water bucket");
        h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(), "Draining preserves the spray already paid for");
        block.useItemOn(player.getMainHandItem(), level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(sprinkler.water() == 975 && player.getMainHandItem().is(Items.BUCKET)
                && player.getInventory().countItem(Items.WATER_BUCKET) == 1, "A partial bucket leaves the held item and tank alone");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void basinWatersAndSuppliesSprinklerAtChunkEdge(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(2048, 100, 4)));
        var basin = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = basin.east();
        var soil = basin.south();
        var head = basin.west();
        level.getChunkAt(basin);
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var block = AEBlocks.IRRIGATION_BASIN.get();
        level.setBlock(basin, block.defaultBlockState(), quietUpdate);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), quietUpdate);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), quietUpdate);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var hit = new BlockHitResult(Vec3.atCenterOf(basin), Direction.UP, basin, false);
        block.useItemOn(player.getMainHandItem(), level.getBlockState(basin), level, basin, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED)
                && level.getBlockState(soil).getValue(FarmBlock.MOISTURE) == 7
                && FarmlandWaterManager.hasBlockWaterTicket(level, soil), "Filling waters the farmland next to the chunk border");
        h.assertFalse(level.hasChunkAt(outside), "Bucket filling and hydration leave the neighboring chunk unloaded");

        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 975 && !level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED),
                "The sprinkler can draw one bucket from the basin");
        h.assertTrue(!FarmlandWaterManager.hasBlockWaterTicket(level, soil) && !level.hasChunkAt(outside),
                "Draining releases the basin's water ticket and leaves the next chunk unloaded");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void basinDrainingKeepsTheEmptyBucketStack(GameTestHelper h) {
        setup(h, true);
        var level = h.getLevel();
        var basin = position(h).west(2);
        var block = AEBlocks.IRRIGATION_BASIN.get();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(basin), Direction.UP, basin, false);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 3));
        block.useItemOn(player.getOffhandItem(), level.getBlockState(basin), level, basin, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(!level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED)
                && player.getOffhandItem().is(Items.BUCKET) && player.getOffhandItem().getCount() == 2
                && player.getInventory().countItem(Items.WATER_BUCKET) == 1, "One bucket drains the basin and the rest stay in the offhand");
        var empty = block.useItemOn(player.getOffhandItem(), level.getBlockState(basin), level, basin, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(empty == ItemInteractionResult.CONSUME && player.getOffhandItem().getCount() == 2
                        && player.getInventory().countItem(Items.WATER_BUCKET) == 1,
                "An empty basin consumes the interaction and leaves the buckets alone");
        level.setBlock(basin, level.getBlockState(basin).setValue(IrrigationBasinBlock.FILLED, true), Block.UPDATE_ALL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        var full = block.useItemOn(player.getOffhandItem(), level.getBlockState(basin), level, basin, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(full == ItemInteractionResult.CONSUME && player.getOffhandItem().is(Items.WATER_BUCKET) && level.isEmptyBlock(basin.above()),
                "From the offhand, a full basin cannot spill the water bucket into the world");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void sprinklerRefillsWithLessThanOneSprayInItsTank(GameTestHelper h) {
        var sprinkler = setup(h, true);
        var level = h.getLevel();
        var basin = position(h).west(2);
        var tag = new CompoundTag();
        tag.putInt("SprinklerWater", 10);
        sprinkler.loadAdditional(tag, level.registryAccess());
        h.assertTrue(sprinkler.inspectionStatus().equals("Connected source ready to refill"), "The status shows a refill is available");
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 985 && sprinkler.growthActive()
                && !level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "A refill keeps the last 10 mB and pays for one spray");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void pipePortsAtChunkEdgeSyncLoadedFittings(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(1536, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var head = edge.west();
        var outside = edge.east();
        level.getChunkAt(edge);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var west = IrrigationSprinklerBlock.connection(Direction.WEST);
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState().setValue(west, true);
        level.setBlock(edge, pipe, quietUpdate);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState().setValue(east, true), quietUpdate);

        h.assertTrue(!IrrigationPipeBlock.toggle(level, edge, Direction.EAST)
                && level.getBlockState(edge).equals(pipe), "A face into an unloaded chunk waits for its neighbor");
        h.assertTrue(IrrigationPipeBlock.toggle(level, edge, Direction.WEST)
                && !level.getBlockState(edge).getValue(west) && !level.getBlockState(head).getValue(east)
                && IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.DISABLED_PORT,
                "Closing the loaded face updates both fittings");
        level.setBlock(edge.below(), AEBlocks.IRRIGATION_VALVE.get().defaultBlockState()
                .setValue(IrrigationValveBlock.OPEN, false), quietUpdate);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        sprinkler.fillBucket();
        sprinkler.irrigateCycle();
        h.assertTrue(level.getBlockState(head).getValue(IrrigationSprinklerBlock.ACTIVE),
                "The isolated sprinkler runs on its own tank");
        h.assertTrue(IrrigationPipeBlock.toggle(level, edge, Direction.WEST)
                && level.getBlockState(edge).getValue(west) && level.getBlockState(head).getValue(east),
                "Reopening the loaded face reconnects both fittings");
        h.assertFalse(level.getBlockState(head).getValue(IrrigationSprinklerBlock.ACTIVE),
                "Reconnecting the closed valve stops the spray immediately");
        h.assertFalse(level.hasChunkAt(outside), "Changing either face leaves the neighboring chunk unloaded");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void emptyBucketDrawsOnlyFromTheSprinklerTank(GameTestHelper h) {
        var sprinkler = setup(h, true);
        var level = h.getLevel();
        var pos = position(h);
        var valve = pos.west();
        var basin = pos.west(2);
        var valveState = level.getBlockState(valve);
        level.setBlock(valve, valveState.setValue(IrrigationValveBlock.OPEN, false), Block.UPDATE_ALL);
        var block = AEBlocks.IRRIGATION_SPRINKLER.get();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
        block.useItemOn(player.getOffhandItem(), level.getBlockState(pos), level, pos, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(sprinkler.water() == 1000 && player.getOffhandItem().is(Items.BUCKET), "A paused sprinkler can store a full offhand bucket");
        block.useItemOn(player.getOffhandItem(), level.getBlockState(pos), level, pos, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(sprinkler.water() == 0 && player.getOffhandItem().is(Items.WATER_BUCKET)
                && level.getBlockState(pos).getAnalogOutputSignal(level, pos) == 0, "Collecting the last full bucket empties the tank");

        level.setBlock(valve, valveState, Block.UPDATE_ALL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
        block.useItemOn(player.getOffhandItem(), level.getBlockState(pos), level, pos, player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(sprinkler.water() == 0 && player.getOffhandItem().is(Items.BUCKET)
                && level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "A bucket only uses water already stored in the sprinkler");
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks = 140)
    public static void manualFillWaitsTwoSecondsForNextSpray(GameTestHelper h) {
        var sprinkler = setup(h, false);
        var level = h.getLevel();
        var pos = position(h);
        var soil = pos.below(2);
        long delay = 40 + Math.floorMod(38 - level.getGameTime(), 40);
        h.runAfterDelay(delay, () -> {
            h.assertTrue(Math.floorMod(level.getGameTime(), 40) == 38, "Fill just before the world watering tick");
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var water = new ItemStack(Items.WATER_BUCKET);
            player.setItemInHand(InteractionHand.MAIN_HAND, water);
            var block = AEBlocks.IRRIGATION_SPRINKLER.get();
            block.useItemOn(water, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive()
                    && level.getBlockState(soil).getValue(FarmBlock.MOISTURE) == 7, "Bucket starts one spray immediately");
            h.runAfterDelay(3, () -> h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(),
                    "The world watering tick must not charge a second spray"));
            h.runAfterDelay(39, () -> h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(),
                    "The first spray and its growth bonus last the full 40 ticks"));
            h.runAfterDelay(41, () -> {
                h.assertTrue(sprinkler.water() == 950 && sprinkler.growthActive(), "The next spray starts 40 ticks after the first");
                h.succeed();
            });
        });
    }

    @GameTest(template="empty")
    public static void sprinklerResumesPaidSprayWhenValveReopens(GameTestHelper h) {
        var sprinkler = setup(h, false);
        var level = h.getLevel();
        var pos = position(h);
        var valve = pos.above();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var block = AEBlocks.IRRIGATION_VALVE.get();
        var hit = new BlockHitResult(Vec3.atCenterOf(valve), Direction.UP, valve, false);
        level.setBlock(valve, block.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.below(2), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        sprinkler.fillBucket();
        sprinkler.startAfterFill();
        h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(), "The first spray costs 25 mB");

        h.runAfterDelay(5, () -> {
            block.useWithoutItem(level.getBlockState(valve), level, valve, player, hit);
            h.assertFalse(sprinkler.growthActive(), "The valve immediately stops the crop boost");
        });
        h.runAfterDelay(10, () -> {
            h.assertTrue(sprinkler.water() == 975 && !sprinkler.growthActive(), "The closed valve holds the spray");
            block.useWithoutItem(level.getBlockState(valve), level, valve, player, hit);
        });
        h.runAfterDelay(12, () -> h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(),
                "The spray resumes without a second charge after the valve opens"));
        h.runAfterDelay(39, () -> h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive(),
                "Resuming keeps the original watering deadline"));
        h.runAfterDelay(41, () -> {
            h.assertTrue(sprinkler.water() == 950 && sprinkler.growthActive(), "The next spray is charged on schedule");
            h.succeed();
        });
    }

    @GameTest(template="empty")
    public static void sprinklerWatersLoadedSoilAtChunkEdge(GameTestHelper h) {
        var level = h.getLevel();
        var isolatedChunk = new ChunkPos(h.absolutePos(new BlockPos(512, 100, 4)));
        var pos = new BlockPos(isolatedChunk.getMaxBlockX(), 100, isolatedChunk.getMiddleBlockZ());
        var outside = pos.east();
        level.getChunkAt(pos);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var soil = pos.below(2);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), quietUpdate);
        level.setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), quietUpdate);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pos);
        sprinkler.fillBucket();
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 975 && sprinkler.growthActive()
                && level.getBlockState(soil).getValue(FarmBlock.MOISTURE) == 7, "Stored water still reaches loaded farmland");
        h.assertFalse(level.hasChunkAt(outside), "Irrigation must not load the neighboring chunk");
        var watering = FarmInspection.describe(level, pos, true, false);
        h.assertTrue(watering.contains("Watering, x5 natural crop growth") && watering.contains("Reachable farmland: 1 / 81"),
                "The coverage panel shows the loaded plot being watered");
        h.assertFalse(level.hasChunkAt(outside), "Inspecting the active head must leave the neighboring chunk unloaded");

        var conductor = pos.west();
        var lever = conductor.below();
        level.setBlock(conductor, Blocks.STONE.defaultBlockState(), quietUpdate);
        level.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.CEILING)
                .setValue(LeverBlock.POWERED, true), quietUpdate);
        sprinkler.checkFlow();
        h.assertTrue(!sprinkler.growthActive() && sprinkler.inspectionStatus().equals("Paused by redstone"),
                "Loaded redstone still pauses the head through a solid block");
        level.setBlock(lever, Blocks.AIR.defaultBlockState(), quietUpdate);

        var connected = level.getBlockState(pos).setValue(IrrigationSprinklerBlock.connection(Direction.EAST), true);
        level.setBlock(pos, connected, quietUpdate);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 975 && sprinkler.inspectionStatus().equals("Connection leads east into an unloaded chunk"),
                "A remembered fitting across the boundary must not be bypassed");
        h.assertTrue(FarmInspection.describe(level, pos, true, false).contains("Connection leads east into an unloaded chunk"),
                "The coverage panel shows the missing connection when it actually stops the spray");
        h.assertFalse(level.hasChunkAt(outside), "Inspecting the connection must leave the neighboring chunk unloaded");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void traceIdentifiesUnloadedConnections(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(768, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = edge.east();
        level.getChunkAt(edge);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var headState = AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState();
        var basin = edge.west();
        level.setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), quietUpdate);

        level.setBlock(edge, headState.setValue(east, true), quietUpdate);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(edge);
        h.assertTrue(sprinkler.inspectionStatus().equals("Connection leads east into an unloaded chunk"), "An adjacent unloaded connection pauses the head");
        var paused = new IrrigationNetwork.Route(List.of(edge), IrrigationNetwork.Flow.UNLOADED_CONNECTION, null, Direction.EAST, -1);
        h.assertTrue(IrrigationNetwork.trace(level, edge).equals(paused), "Show the sprinkler's unavailable connection even with a full basin");

        var head = edge.above();
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        level.setBlock(edge, pipe.setValue(east, true), quietUpdate);
        level.setBlock(head, headState, quietUpdate);
        var loadedSupply = new IrrigationNetwork.Route(List.of(head, edge, basin), IrrigationNetwork.Flow.READY, null, null, 1000);
        h.assertTrue(IrrigationNetwork.trace(level, head).equals(loadedSupply), "A remote unloaded branch can be bypassed by a loaded supply");
        h.assertTrue(level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "Tracing leaves the basin full");

        level.setBlock(basin, Blocks.AIR.defaultBlockState(), quietUpdate);
        var border = new IrrigationNetwork.Route(List.of(head, edge), IrrigationNetwork.Flow.UNLOADED_CONNECTION, null, Direction.EAST, -1);
        h.assertTrue(IrrigationNetwork.trace(level, head).equals(border), "The route ends at the last loaded fitting");
        var remoteHead = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        h.assertTrue(remoteHead.inspectionStatus().equals("Refill blocked: Connection leads east into an unloaded chunk"),
                "The sprinkler names the unavailable refill path without loading it");
        level.setBlock(edge, pipe, quietUpdate);
        h.assertTrue(IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.DISCONNECTED,
                "An unused port says disconnected even when it faces an unloaded chunk");
        h.assertTrue(IrrigationNetwork.trace(level, outside).equals(
                        new IrrigationNetwork.Route(List.of(outside), IrrigationNetwork.Flow.UNLOADED_CONNECTION, null))
                        && !IrrigationNetwork.search(level, outside).blockedValve(),
                "An unloaded origin reports an unavailable chunk rather than a closed valve");
        h.assertFalse(level.hasChunkAt(outside), "Tracing must leave the neighboring chunk unloaded");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void spannerInspectsValveWithoutLoadingNeighbor(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(1024, 100, 4)));
        var valve = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = valve.east();
        level.getChunkAt(valve);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk must start unloaded");
        var state = AEBlocks.IRRIGATION_VALVE.get().defaultBlockState()
                .setValue(IrrigationSprinklerBlock.connection(Direction.EAST), true);
        level.setBlock(valve, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.SPANNER.get()));
        h.assertTrue(level.mayInteract(player, valve), "The player must be allowed to inspect the valve");
        var hit = new BlockHitResult(Vec3.atCenterOf(valve), Direction.UP, valve, false);
        var interaction = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, valve, hit);
        SpannerItem.onRightClick(interaction);

        h.assertTrue(interaction.isCanceled(), "The Spanner should handle the interaction");
        h.assertFalse(level.hasChunkAt(outside), "Inspecting the open valve must leave the neighboring chunk unloaded");
        var zone = IrrigationNetwork.inspectZone(level, valve);
        h.assertTrue(zone.supplies().isEmpty() && zone.unloadedConnection() && !zone.blockedPath()
                        && zone.unloaded().side() == Direction.EAST
                        && SpannerItem.describeZone(level, valve, state, "Garden").getFirst()
                        .contains("Connection leads east into an unloaded chunk [" + valve.toShortString() + "]"),
                "An unloaded supply connection is distinct from a shut valve or a disabled port");
        h.assertTrue(level.getBlockState(valve).getValue(IrrigationValveBlock.OPEN), "Inspecting must leave the valve open");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void savedIrrigationFittingsFollowStructureTransforms(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(4, 170, 4));
        var relativeHead = new BlockPos(0, 0, 1);
        var head = origin.offset(relativeHead);
        var pipe = head.east();
        var valve = pipe.east();
        var basin = AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pipe.above(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState()
                .setValue(IrrigationValveBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        level.setBlock(valve.east(), basin, Block.UPDATE_ALL);
        level.setBlock(pipe.north(), basin, Block.UPDATE_ALL);
        IrrigationPipeBlock.toggle(level, pipe, Direction.NORTH);

        var template = new StructureTemplate();
        template.fillFromWorld(level, origin, new Vec3i(4, 2, 2), false, null);
        var transformations = List.of(
                new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90),
                new StructurePlaceSettings().setMirror(Mirror.LEFT_RIGHT),
                new StructurePlaceSettings().setMirror(Mirror.FRONT_BACK).setRotation(Rotation.COUNTERCLOCKWISE_90));
        int height = 4;
        for (var placement : transformations) {
            var target = origin.above(height);
            height += 4;
            h.assertTrue(template.placeInWorld(level, target, target, placement.setKnownShape(true), level.random,
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE), "The saved structure can be placed");

            Direction forward = placement.getRotation().rotate(placement.getMirror().mirror(Direction.EAST));
            Direction branch = placement.getRotation().rotate(placement.getMirror().mirror(Direction.NORTH));
            var pastedHead = StructureTemplate.calculateRelativePosition(placement, relativeHead).offset(target);
            var pastedPipe = pastedHead.relative(forward);
            var pastedValve = pastedPipe.relative(forward);
            var suppliedBasin = pastedValve.relative(forward);
            var blockedBasin = pastedPipe.relative(branch);
            var pipeState = level.getBlockState(pastedPipe);
            var valveState = level.getBlockState(pastedValve);
            h.assertTrue(level.getBlockState(pastedHead).getValue(IrrigationSprinklerBlock.connection(forward))
                    && pipeState.getValue(IrrigationSprinklerBlock.connection(forward.getOpposite()))
                    && pipeState.getValue(IrrigationSprinklerBlock.connection(Direction.UP))
                    && valveState.getValue(IrrigationValveBlock.FACING) == forward
                    && valveState.getValue(IrrigationSprinklerBlock.connection(forward)),
                    "The visible joins and the valve facing follow the saved layout");
            h.assertFalse(IrrigationPipeBlock.enabled(pipeState, branch), "The disabled port still faces the blocked basin");
            var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(pastedHead);
            sprinkler.irrigateCycle();
            h.assertTrue(sprinkler.water() == 975 && !level.getBlockState(suppliedBasin).getValue(IrrigationBasinBlock.FILLED)
                    && level.getBlockState(blockedBasin).getValue(IrrigationBasinBlock.FILLED),
                    "Water comes from the intended basin while the blocked branch stays full");
        }
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
    public static void fittingPlacementChecksOnlyLoadedConnections(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(2304, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var support = edge.west();
        level.getChunkAt(edge);
        var pipe = AEBlocks.IRRIGATION_PIPE.get();
        var head = AEBlocks.IRRIGATION_SPRINKLER.get();
        level.setBlock(support, pipe.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.EAST, support, false);
        h.assertFalse(level.hasChunkAt(edge.east()), "The neighboring chunk must start unloaded");

        var pipeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(pipe), hit);
        var headContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(head), hit);
        h.assertTrue(pipeContext.getClickedPos().equals(edge) && headContext.getClickedPos().equals(edge), "Both placements target the edge");
        for (var state : List.of(pipe.getStateForPlacement(pipeContext), head.getStateForPlacement(headContext))) {
            h.assertTrue(state.getValue(IrrigationSprinklerBlock.connection(Direction.WEST))
                    && !state.getValue(IrrigationSprinklerBlock.connection(Direction.EAST)), "The loaded fitting provides the only connection");
        }
        h.assertFalse(level.hasChunkAt(edge.east()), "Placement connection checks must leave the neighboring chunk unloaded");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placingIrrigationItemsConnectsWithoutLoadingTheNextChunk(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(5888, 100, 4)));
        var edge = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var outside = edge.east();
        var neighbor = edge.west();
        var oldBasinPos = edge.west(3);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.getChunkAt(edge);
        level.setBlock(edge.below(), Blocks.STONE.defaultBlockState(), quiet);
        level.setBlock(neighbor, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quiet);
        var oldBasinState = AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.UPGRADE, 2).setValue(IrrigationBasinBlock.FILLED, true);
        level.setBlock(oldBasinPos, oldBasinState, quiet);
        var basinItem = Block.getDrops(oldBasinState, level, oldBasinPos, level.getBlockEntity(oldBasinPos)).getFirst();
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk starts unloaded");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(edge), Direction.UP, edge.below(), false);
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var west = IrrigationSprinklerBlock.connection(Direction.WEST);
        var placements = List.of(new ItemStack(AEItems.IRRIGATION_PIPE.get()), new ItemStack(AEItems.IRRIGATION_VALVE.get()),
                new ItemStack(AEItems.IRRIGATION_SPRINKLER.get()), basinItem);
        for (var stack : placements) {
            var block = ((BlockItem) stack.getItem()).getBlock();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            h.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction() && stack.isEmpty(),
                    "Place one " + block.getDescriptionId() + " from the player's hand");
            var placed = level.getBlockState(edge);
            h.assertTrue(placed.is(block) && level.getBlockState(neighbor).getValue(east) && !level.hasChunkAt(outside),
                    block.getDescriptionId() + " connects on placement without loading the other chunk");
            if (placed.hasProperty(west)) {
                h.assertTrue(placed.getValue(west) && !placed.getValue(east), "The new fitting only connects to the loaded pipe");
            }
            if (level.getBlockEntity(edge) instanceof IrrigationBasinBlockEntity basin) {
                h.assertTrue(basin.upgradeTier() == 2 && placed.getValue(IrrigationBasinBlock.UPGRADE) == 2
                                && !placed.getValue(IrrigationBasinBlock.FILLED), "The moved Mk III basin starts dry at its saved tier");
            }
            level.setBlock(edge, Blocks.AIR.defaultBlockState(), quiet);
            h.assertTrue(!level.getBlockState(neighbor).getValue(east) && !level.hasChunkAt(outside), "Removal clears the arm before the next placement");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void valveControlsLeaveTheNeighboringChunkUnloaded(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(2560, 100, 4)));
        var valve = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var head = valve.west();
        var outside = valve.east();
        level.getChunkAt(valve);
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        var block = AEBlocks.IRRIGATION_VALVE.get();
        level.setBlock(valve, block.defaultBlockState(), quietUpdate);
        level.setBlock(head, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), quietUpdate);
        var sprinkler = (IrrigationSprinklerBlockEntity) level.getBlockEntity(head);
        sprinkler.fillBucket();
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.growthActive() && !level.hasChunkAt(outside), "The sprinkler starts with the neighboring chunk unloaded");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(valve), Direction.UP, valve, false);
        block.useWithoutItem(level.getBlockState(valve), level, valve, player, hit);
        h.assertTrue(!sprinkler.growthActive() && !level.hasChunkAt(outside), "Manually closing the valve stops the head without loading a chunk");
        block.useWithoutItem(level.getBlockState(valve), level, valve, player, hit);
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.growthActive(), "The head can run with the valve open");
        level.setBlock(valve.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), quietUpdate);
        block.neighborChanged(level.getBlockState(valve), level, valve, Blocks.REDSTONE_BLOCK, valve.above(), false);
        h.assertTrue(level.getBlockState(valve).getValue(IrrigationValveBlock.POWERED)
                && !sprinkler.growthActive() && !level.hasChunkAt(outside), "Redstone stops the head without loading a chunk");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void valvePlacedFirstAutoAligns(GameTestHelper h) {
        var level=h.getLevel(); var pos=position(h); var valve=pos.above();
        level.setBlock(valve,AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(),3);
        var originalFacing = level.getBlockState(valve).getValue(IrrigationValveBlock.FACING);
        level.setBlock(valve.relative(originalFacing), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler=setup(h,false);
        h.assertTrue(level.getBlockState(valve).getValue(IrrigationValveBlock.FACING).getAxis()==Direction.Axis.Y,
                "Adding a sprinkler below takes priority over a pipe on the original axis");
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
