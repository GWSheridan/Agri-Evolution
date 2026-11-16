package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class ValveJunctionGameTests {
    @GameTest(template = "empty")
    public static void loadedValvesUseTheirCurrentRedstoneSignal(GameTestHelper h) {
        var level = h.getLevel();
        var unpowered = h.absolutePos(new BlockPos(3, 165, 3));
        var powered = unpowered.east(3);
        var valve = AEBlocks.IRRIGATION_VALVE.get().defaultBlockState();
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(powered.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), quietUpdate);
        level.setBlock(unpowered.below(), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), quietUpdate);
        level.setBlock(powered, valve, quietUpdate);
        level.setBlock(unpowered, valve.setValue(IrrigationValveBlock.POWERED, true), quietUpdate);
        h.assertTrue(!level.getBlockState(powered).getValue(IrrigationValveBlock.POWERED)
                && level.getBlockState(unpowered).getValue(IrrigationValveBlock.POWERED), "Both valves start with stale power flags");
        h.runAfterDelay(5, () -> {
            h.assertTrue(level.getBlockState(powered).getValue(IrrigationValveBlock.POWERED)
                    && !level.getBlockState(unpowered).getValue(IrrigationValveBlock.POWERED), "Loaded valves check the current signal");
            h.assertTrue(level.getBlockState(unpowered).getValue(IrrigationValveBlock.FACING) == Direction.DOWN
                            && level.getBlockState(unpowered).getValue(IrrigationSprinklerBlock.connection(Direction.DOWN))
                            && level.getBlockState(unpowered.below()).getValue(IrrigationSprinklerBlock.connection(Direction.UP)),
                    "The load tick repairs saved alignment and connectors on both fittings");
            h.assertTrue(FarmInspection.describe(level, powered, true, false).contains("Valve paused by redstone"),
                    "The Spanner distinguishes a powered valve from a manual shutoff");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void loadedValveRepairsTheOtherEndOfAnAlreadyConnectedPort(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 166, 4));
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        var west = IrrigationSprinklerBlock.connection(Direction.WEST);
        var state = AEBlocks.IRRIGATION_VALVE.get().defaultBlockState().setValue(IrrigationValveBlock.FACING, Direction.EAST)
                .setValue(IrrigationValveBlock.OPEN, false).setValue(east, true);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos.east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quiet);
        level.setBlock(pos, state, quiet);
        var valve = (IrrigationValveBlockEntity) level.getBlockEntity(pos);
        valve.rename("West beds");
        h.runAfterDelay(5, () -> {
            h.assertTrue(level.getBlockState(pos) == state && level.getBlockState(pos.east()).getValue(west)
                            && valve.zone().equals("West beds"),
                    "The load tick fixes the pipe even when the valve already has the correct state and zone name");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void valveAlignsToUsablePorts(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 168, 4));
        var pipe = pos.north();
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pipe, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quietUpdate);
        h.assertTrue(IrrigationPipeBlock.toggle(level, pipe, Direction.SOUTH), "Close the pipe face before placement");
        level.setBlock(pos.above(), AEBlocks.WATER_RESERVOIR.get().defaultBlockState().setValue(WaterReservoirBlock.X, 0), quietUpdate);
        level.setBlock(pos.east(), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState(), quietUpdate);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), quietUpdate);
        var valve = AEBlocks.IRRIGATION_VALVE.get();
        var context = new BlockPlaceContext(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), InteractionHand.MAIN_HAND,
                new ItemStack(valve), new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
        var placed = valve.getStateForPlacement(context);
        h.assertTrue(placed.getValue(IrrigationValveBlock.FACING).getAxis() == Direction.Axis.X
                        && placed.getValue(IrrigationSprinklerBlock.connection(Direction.EAST))
                        && !placed.getValue(IrrigationSprinklerBlock.connection(Direction.NORTH))
                        && !placed.getValue(IrrigationSprinklerBlock.connection(Direction.UP)),
                "The valve aligns with the basin and ignores the disabled pipe and reservoir casing");
        level.setBlock(pos, placed, Block.UPDATE_ALL);
        IrrigationPipeBlock.toggle(level, pipe, Direction.SOUTH);
        h.assertTrue(level.getBlockState(pos).getValue(IrrigationValveBlock.FACING).getAxis() == Direction.Axis.Z,
                "An enabled pipe takes alignment priority over a basin");
        IrrigationPipeBlock.toggle(level, pipe, Direction.SOUTH);
        h.assertTrue(level.getBlockState(pos).getValue(IrrigationValveBlock.FACING).getAxis() == Direction.Axis.X,
                "Closing the pipe face restores alignment to the basin");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spannerSkipsClosedPortsBypassedByLoops(GameTestHelper h) {
        var level = h.getLevel();
        var valve = h.absolutePos(new BlockPos(4, 173, 4));
        var corner = valve.east().south();
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (var pos : new BlockPos[] {valve.east(), valve.south(), corner}) {
            level.setBlock(pos, pipe, Block.UPDATE_ALL);
        }
        IrrigationPipeBlock.toggle(level, valve.south(), Direction.EAST);
        h.assertTrue(IrrigationNetwork.trace(level, valve).flow() == IrrigationNetwork.Flow.DISCONNECTED
                        && !IrrigationNetwork.inspectZone(level, valve).blockedPath(),
                "The closed face is bypassed and the network has no source");

        level.setBlock(corner.below(), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState(), Block.UPDATE_ALL);
        IrrigationPipeBlock.toggle(level, corner, Direction.DOWN);
        var route = IrrigationNetwork.trace(level, valve);
        h.assertTrue(route.flow() == IrrigationNetwork.Flow.DISABLED_PORT && route.blocks().getLast().equals(corner)
                        && route.disabledSide() == Direction.DOWN && IrrigationNetwork.inspectZone(level, valve).blockedPath(),
                "The Spanner identifies the face that isolates the basin");
        h.assertTrue(SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden").getFirst()
                        .contains("Supply blocked by disabled down pipe port"), "The zone report identifies the same blocked face");

        var shutoff = corner.below();
        var valveState = AEBlocks.IRRIGATION_VALVE.get().defaultBlockState();
        level.setBlock(shutoff, valveState.setValue(IrrigationValveBlock.OPEN, false), Block.UPDATE_ALL);
        IrrigationPipeBlock.toggle(level, corner, Direction.DOWN);
        h.assertTrue(SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden").getFirst()
                        .contains("Valve closed along supply path"), "The zone locates a remote manual shutoff");
        level.setBlock(shutoff, valveState, Block.UPDATE_ALL);
        level.setBlock(shutoff.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(SpannerItem.describeZone(level, valve, level.getBlockState(valve), "Garden").getFirst()
                        .contains("Valve paused by redstone along supply path"), "The zone distinguishes a powered remote valve");
        h.succeed();
    }

    @GameTest(template="empty") public static void refreshExistingValveAndPipeModels(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,20,4));
        level.setBlock(pos.below(),AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),2);
        level.setBlock(pos.west(),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),2);
        level.setBlock(pos.east(),AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),2);
        // Simulate old saved blocks with no side-connection properties set.
        var valve=AEBlocks.IRRIGATION_VALVE.get();
        level.setBlock(pos,valve.defaultBlockState().setValue(IrrigationValveBlock.FACING,Direction.DOWN),2);
        valve.useWithoutItem(level.getBlockState(pos),level,pos,h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),
                new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
        for(var d:new Direction[]{Direction.WEST,Direction.EAST}) {
            h.assertTrue(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(d)),"Click refreshes saved valve connector");
            h.assertTrue(level.getBlockState(pos.relative(d)).getValue(IrrigationSprinklerBlock.connection(d.getOpposite())),"Click refreshes saved pipe connector");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void sprinklerFirstValveThenSidePipes(GameTestHelper h) {
        var level=h.getLevel();var headPos=h.absolutePos(new BlockPos(4,20,4));var valvePos=headPos.above();
        level.setBlock(headPos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(AEBlocks.IRRIGATION_VALVE.get()),
                new BlockHitResult(Vec3.atCenterOf(headPos).add(0,.5,0),Direction.UP,headPos,false));
        level.setBlock(valvePos,AEBlocks.IRRIGATION_VALVE.get().getStateForPlacement(context),3);
        h.assertTrue(level.getBlockState(valvePos).getValue(IrrigationValveBlock.FACING)==Direction.DOWN,"Valve must initially face the sprinkler below");
        for(var d:Direction.values())if(d!=Direction.DOWN) {
            var p=valvePos.relative(d);
            level.setBlock(p,Block.updateFromNeighbourShapes(AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(),level,p),3);
            h.assertTrue(level.getBlockState(p).getValue(IrrigationSprinklerBlock.connection(d.getOpposite())),"Pipe visually connects to valve "+d);
            h.assertTrue(level.getBlockState(valvePos).getValue(IrrigationSprinklerBlock.connection(d)),"Valve visually connects to pipe "+d);
        }
        var supply=valvePos.west(2);
        level.setBlock(supply,AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED,true),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(headPos);head.irrigateCycle();
        h.assertTrue(head.growthActive()&&head.water()==975,"Side supply must feed the downward sprinkler");
        level.setBlock(valvePos,level.getBlockState(valvePos).setValue(IrrigationValveBlock.OPEN,false),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Closing junction must stop all branches");
        level.setBlock(valvePos,level.getBlockState(valvePos).setValue(IrrigationValveBlock.OPEN,true),3);
        level.setBlock(valvePos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);head.checkFlow();
        h.assertFalse(head.growthActive(),"Redstone must stop side-fed sprinkler");
        level.setBlock(valvePos.above(),Blocks.AIR.defaultBlockState(),3);head.irrigateCycle();
        h.assertTrue(head.growthActive(),"Removing signal resumes paid water buffer");
        level.setBlock(valvePos.west(),Blocks.AIR.defaultBlockState(),3);head.checkFlow();
        h.assertFalse(level.getBlockState(valvePos).getValue(IrrigationSprinklerBlock.connection(Direction.WEST)),"Removing pipe removes valve connector");
        h.succeed();
    }
}
