package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
        var supplyRoute = new IrrigationNetwork.Route(List.of(pos, pipe, basin), IrrigationNetwork.Flow.READY, null);

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
        h.assertTrue(sprinkler.water() == 975 && sprinkler.inspectionStatus().equals("Connected chunk not loaded"),
                "A remembered fitting across the boundary must not be bypassed");
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
        h.assertTrue(sprinkler.inspectionStatus().equals("Connected chunk not loaded"), "An adjacent unloaded connection pauses the head");
        var paused = new IrrigationNetwork.Route(List.of(edge), IrrigationNetwork.Flow.UNLOADED_CONNECTION, null);
        h.assertTrue(IrrigationNetwork.trace(level, edge).equals(paused), "Show the sprinkler's unavailable connection even with a full basin");

        var head = edge.above();
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        level.setBlock(edge, pipe.setValue(east, true), quietUpdate);
        level.setBlock(head, headState, quietUpdate);
        var loadedSupply = new IrrigationNetwork.Route(List.of(head, edge, basin), IrrigationNetwork.Flow.READY, null);
        h.assertTrue(IrrigationNetwork.trace(level, head).equals(loadedSupply), "A remote unloaded branch can be bypassed by a loaded supply");
        h.assertTrue(level.getBlockState(basin).getValue(IrrigationBasinBlock.FILLED), "Tracing leaves the basin full");

        level.setBlock(basin, Blocks.AIR.defaultBlockState(), quietUpdate);
        var border = new IrrigationNetwork.Route(List.of(head, edge), IrrigationNetwork.Flow.UNLOADED_CONNECTION, null);
        h.assertTrue(IrrigationNetwork.trace(level, head).equals(border), "The route ends at the last loaded fitting");
        level.setBlock(edge, pipe, quietUpdate);
        h.assertTrue(IrrigationNetwork.trace(level, head).flow() == IrrigationNetwork.Flow.DISCONNECTED,
                "An unused port says disconnected even when it faces an unloaded chunk");
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
        h.assertTrue(zone.supplies().isEmpty() && zone.unloadedConnection() && !zone.blockedPath(),
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
