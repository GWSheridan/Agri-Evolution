package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
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
        var head=head(h,pos);
        var vanilla=pos.east(); var agri=pos.south(); var outside=pos.east(5);
        for(var p:new BlockPos[]{vanilla,agri,outside}) {
            level.setBlock(p.below(),Blocks.FARMLAND.defaultBlockState(),3);
            level.setBlock(p.above(2),Blocks.GLOWSTONE.defaultBlockState(),3);
            level.setBlock(p,Blocks.WHEAT.defaultBlockState(),3);
        }
        level.setBlock(agri,AEBlocks.AGRI_CROP.get().defaultBlockState(),3);
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
}
