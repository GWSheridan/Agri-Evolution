package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class PipeConnectionGameTests {
    @GameTest(template = "empty")
    public static void quietJunctionPlacementAndRemovalRepairConnectors(GameTestHelper h) {
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(4, 240, 4));
        var pipe = AEBlocks.IRRIGATION_PIPE.get().defaultBlockState();
        var north = IrrigationSprinklerBlock.connection(Direction.NORTH);
        var south = IrrigationSprinklerBlock.connection(Direction.SOUTH);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(center.north(), pipe, quiet);
        h.assertTrue(IrrigationPipeBlock.toggle(level, center.north(), Direction.SOUTH), "Cap the north branch first");
        level.setBlock(center.west(), pipe, quiet);
        level.setBlock(center.east(), AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), quiet);
        level.setBlock(center, pipe, quiet);

        h.runAfterDelay(5, () -> {
            var east = IrrigationSprinklerBlock.connection(Direction.EAST);
            var west = IrrigationSprinklerBlock.connection(Direction.WEST);
            var junction = level.getBlockState(center);
            h.assertTrue(junction.getValue(east) && junction.getValue(west)
                            && level.getBlockState(center.west()).getValue(east)
                            && level.getBlockState(center.east()).getValue(west),
                    "The junction and both adjoining fittings repair their connectors");
            var capped = level.getBlockState(center.north());
            h.assertTrue(!junction.getValue(north) && !capped.getValue(south)
                            && !IrrigationPipeBlock.enabled(capped, Direction.SOUTH),
                    "The repair respects a disabled port on the neighboring pipe");
            level.setBlock(center, Blocks.AIR.defaultBlockState(), quiet);
            h.assertTrue(!level.getBlockState(center.west()).getValue(east)
                            && !level.getBlockState(center.east()).getValue(west),
                    "Quiet removal clears both adjoining arms immediately");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void basinPlacementAndRemovalKeepChunkEdgePipeInSync(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(4864, 100, 4)));
        var basinPos = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var pipePos = basinPos.west();
        var outside = basinPos.east();
        var east = IrrigationSprinklerBlock.connection(Direction.EAST);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.getChunkAt(basinPos);
        h.assertFalse(level.hasChunkAt(outside), "The neighboring chunk starts unloaded");
        level.setBlock(pipePos, AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), quiet);
        level.setBlock(basinPos, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState()
                .setValue(IrrigationBasinBlock.FILLED, true), quiet);
        h.assertTrue(level.getBlockState(pipePos).getValue(east) && IrrigationNetwork.trace(level, pipePos).flow() == IrrigationNetwork.Flow.READY,
                "The pipe connects to the new basin and sees its water");
        level.setBlock(basinPos, Blocks.AIR.defaultBlockState(), quiet);
        h.assertTrue(!level.getBlockState(pipePos).getValue(east) && IrrigationNetwork.trace(level, pipePos).flow() == IrrigationNetwork.Flow.DISCONNECTED,
                "Removing the basin clears the arm and the source");
        h.assertFalse(level.hasChunkAt(outside), "Placement, inspection and removal leave the neighboring chunk unloaded");
        h.succeed();
    }

    @GameTest(template="empty") public static void sixWayConnectionsAndRemoval(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(4,20,4));
        var pipe=AEBlocks.IRRIGATION_PIPE.get();
        level.setBlock(center,pipe.defaultBlockState(),3);
        for(var d:Direction.values()) {
            var pos=center.relative(d);
            level.setBlock(pos,Block.updateFromNeighbourShapes(pipe.defaultBlockState(),level,pos),3);
            h.assertTrue(level.getBlockState(center).getValue(IrrigationSprinklerBlock.connection(d)),"Centre must connect "+d);
            h.assertTrue(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.connection(d.getOpposite())),"Arm must connect back "+d);
        }
        // Replace the centre last, as when inserting a junction into an existing build.
        level.setBlock(center,Blocks.AIR.defaultBlockState(),3);
        level.setBlock(center,Block.updateFromNeighbourShapes(pipe.defaultBlockState(),level,center),3);
        for(var d:Direction.values()) {
            h.assertTrue(level.getBlockState(center).getValue(IrrigationSprinklerBlock.connection(d)),"Inserted junction must connect "+d);
            level.setBlock(center.relative(d),Blocks.AIR.defaultBlockState(),3);
            h.assertFalse(level.getBlockState(center).getValue(IrrigationSprinklerBlock.connection(d)),"Removed branch must disconnect "+d);
        }
        h.succeed();
    }
}
