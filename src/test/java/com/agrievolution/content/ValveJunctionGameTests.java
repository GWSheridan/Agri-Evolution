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
