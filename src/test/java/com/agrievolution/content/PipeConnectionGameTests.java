package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("agri_evolution") @PrefixGameTestTemplate(false)
public final class PipeConnectionGameTests {
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
