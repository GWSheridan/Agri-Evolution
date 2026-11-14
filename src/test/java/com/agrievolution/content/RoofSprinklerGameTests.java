package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class RoofSprinklerGameTests {
    @GameTest(template="empty")
    public static void roofReplacementKeepsGreenhouseValid(GameTestHelper h) {
        var level=h.getLevel(); var controller=h.absolutePos(new BlockPos(4,40,1)); var facing=Direction.NORTH;
        for(var cell:BotanicalGreenhouseLayout.previewCells(controller,facing))
            level.setBlock(cell.pos(),cell.part()==BotanicalGreenhouseLayout.PreviewPart.OPTIONAL_FARMLAND
                    ?Blocks.FARMLAND.defaultBlockState():Blocks.GLASS.defaultBlockState(),3);
        var door=BotanicalGreenhouseLayout.at(controller,facing,1,0,0);
        level.setBlock(door,Blocks.OAK_DOOR.defaultBlockState(),2);
        level.setBlock(door.above(),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        level.setBlock(controller,AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING,facing),3);
        h.assertTrue(BotanicalGreenhouseLayout.isComplete(level,controller,facing),"Baseline greenhouse must form");
        var roof=BotanicalGreenhouseLayout.at(controller,facing,0,3,2);
        level.setBlock(roof,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        h.assertTrue(BotanicalGreenhouseLayout.isComplete(level,controller,facing),"Roof sprinkler must seal greenhouse");
        h.assertTrue(BotanicalGreenhouseLayout.previewMatches(level.getBlockState(roof),BotanicalGreenhouseLayout.PreviewPart.ROOF_GLASS),"Preview must recognize roof fitting");
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(roof);
        head.fillBucket();head.irrigateCycle();
        h.assertTrue(head.wateredSoils().size()==25,"Roof head must reach all 25 greenhouse beds");
        h.assertTrue(SprinklerGrowth.coveredPlots(level).contains(roof.below(2)),"Roof bonus must reach crop layer");
        level.setBlock(roof,Blocks.AIR.defaultBlockState(),3);
        h.assertFalse(BotanicalGreenhouseLayout.isComplete(level,controller,facing),"Missing roof fitting must invalidate greenhouse");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void ceilingCoverageStopsAtObstructionsAndDepthLimit(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,60,4));
        level.setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)level.getBlockEntity(pos);
        var reachable=pos.east().below(8);var tooDeep=pos.west().below(9);var blocked=pos.south().below(3);
        for(var soil:new BlockPos[]{reachable,tooDeep,blocked})level.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,0),3);
        level.setBlock(blocked.above(2),Blocks.GLASS.defaultBlockState(),3);
        head.fillBucket();head.irrigateCycle();
        h.assertTrue(level.getBlockState(reachable).getValue(FarmBlock.MOISTURE)==7,"Eight-block drop must reach farmland");
        h.assertTrue(level.getBlockState(tooDeep).getValue(FarmBlock.MOISTURE)==0,"Nine-block drop is out of range");
        h.assertTrue(level.getBlockState(blocked).getValue(FarmBlock.MOISTURE)==0,"Solid glass must block watering");
        h.assertFalse(SprinklerGrowth.coveredPlots(level).contains(blocked.above()),"Obstruction must also block growth bonus");
        h.succeed();
    }
}
