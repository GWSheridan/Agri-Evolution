package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SprinklerGrowthGameTests {
    private static IrrigationSprinklerBlockEntity head(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos,AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(),3);
        var head=(IrrigationSprinklerBlockEntity)h.getLevel().getBlockEntity(pos);
        head.fillBucket(); head.irrigateCycle(); return head;
    }
    @GameTest(template="empty")
    public static void fiveTimesNaturalBudget(GameTestHelper h) {
        var random=RandomSource.create(312L);
        h.assertTrue(SprinklerGrowth.extraTicks(0,random)==0,"Disabled random ticks must disable extra growth");
        h.assertTrue(SprinklerGrowth.extraTicks(4096,random)==160,"40 ticks at one natural tick/block need four extra each");
        int attempts=0;
        for(int i=0;i<100000;i++) attempts+=SprinklerGrowth.extraTicks(3,random);
        h.assertTrue(Math.abs(attempts-11718.75)<250,"Default randomTickSpeed must yield four extra natural opportunities, not five extra");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void overlapAndShutdown(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(4,30,4));
        for(int x=-4;x<=6;x++) for(int z=-4;z<=4;z++)
            level.setBlock(pos.offset(x,-1,z),Blocks.FARMLAND.defaultBlockState(),3);
        var first=head(h,pos); var second=head(h,pos.east(2));
        long count=SprinklerGrowth.coveredPlots(level).stream().filter(p->p.getY()==pos.getY()).count();
        h.assertTrue(count==99,"Two overlapping 9x9 areas two blocks apart must cover 99 unique plots");
        level.setBlock(pos.west(),AEBlocks.IRRIGATION_VALVE.get().defaultBlockState()
                .setValue(IrrigationValveBlock.FACING,Direction.EAST).setValue(IrrigationValveBlock.OPEN,false),3);
        first.checkFlow();
        h.assertFalse(SprinklerGrowth.coveredPlots(level).contains(pos.west(4)),"Closed valve must remove first head's exclusive growth coverage");
        second.onChunkUnloaded();
        h.assertFalse(SprinklerGrowth.coveredPlots(level).contains(pos.east(6)),"Unloaded head must not retain growth coverage");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void growsCropsAndRespectsRange(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(4,50,4));
        var vanilla=pos.east(); var agri=pos.south(); var outside=pos.east(5);
        for(var p:new BlockPos[]{vanilla,agri,outside}) {
            level.setBlock(p.below(),Blocks.FARMLAND.defaultBlockState(),3);
            level.setBlock(p.above(2),Blocks.GLOWSTONE.defaultBlockState(),3);
            level.setBlock(p,Blocks.WHEAT.defaultBlockState(),3);
        }
        level.setBlock(agri,AEBlocks.AGRI_CROP.get().defaultBlockState(),3);
        var head=head(h,pos);
        var rule=level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING); int old=rule.get();
        try {
            rule.set(0,level.getServer()); SprinklerGrowth.boost(level);
            h.assertTrue(level.getBlockState(vanilla).getValue(CropBlock.AGE)==0,"Zero randomTickSpeed prevents boost");
            rule.set(4096,level.getServer()); SprinklerGrowth.boost(level);
            h.assertTrue(level.getBlockState(vanilla).getValue(CropBlock.AGE)>0,"Vanilla crop should grow using its own tick logic");
            h.assertTrue(level.getBlockState(agri).getValue(CropBlock.AGE)>0,"Agri crop should grow using its own tick logic");
            h.assertTrue(level.getBlockState(outside).getValue(CropBlock.AGE)==0,"Outside crop must not receive extra ticks");
        } finally { rule.set(old,level.getServer()); head.setRemoved(); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void growthRequiresWaterFromTheCurrentSpray(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 72, 4));
        var watered = pos.east().below(3);
        var added = pos.south().below(3);
        level.setBlock(watered, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        h.assertTrue(SprinklerGrowth.covers(level, watered.above()), "The first spray covers its farmland");

        level.setBlock(added, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(sprinkler.wateredSoils().contains(added) && !SprinklerGrowth.covers(level, added.above()),
                "The preview can see new farmland before a spray waters it");
        level.setBlock(watered.above(2), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        var plots = SprinklerGrowth.coveredPlots(level);
        h.assertTrue(!SprinklerGrowth.covers(level, watered.above()) && !plots.contains(watered.above()) && !plots.contains(added.above()),
                "Blocked and newly added farmland both wait");
        h.assertTrue(sprinkler.inspectionStatus().equals("Reachable farmland waits for the next spray"),
                "The report explains why the reachable farmland has no bonus");

        sprinkler.irrigateCycle();
        h.assertTrue(SprinklerGrowth.covers(level, added.above()) && SprinklerGrowth.coveredPlots(level).contains(added.above()),
                "The next spray starts the bonus for the new plot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void quickRemoteValveSwitchPreservesPaidGrowth(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 78, 4));
        var soil = pos.south().below(2);
        var valve = pos.east(2);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        level.setBlock(pos.east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos.east(3), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState(), Block.UPDATE_ALL);
        var open = level.getBlockState(valve);
        int quietUpdate = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(valve, open.setValue(IrrigationValveBlock.OPEN, false), quietUpdate);
        h.assertFalse(SprinklerGrowth.coveredPlots(level).contains(soil.above()), "The remote shutoff immediately blocks the bonus");
        level.setBlock(valve, open, quietUpdate);
        h.assertTrue(SprinklerGrowth.covers(level, soil.above()) && SprinklerGrowth.coveredPlots(level).contains(soil.above())
                && sprinkler.water() == 975, "Reopening before the next sprinkler tick restores the paid bonus");
        h.succeed();
    }
}
