package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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

    @GameTest(template = "empty")
    public static void manualValveAndSpannerResumeTheLastPaidSprayImmediately(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 96, 4));
        var soil = pos.south().below(3);
        var valvePos = pos.east();
        var valve = AEBlocks.IRRIGATION_VALVE.get();
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valvePos, valve.defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        for (int cycle = 1; cycle < 40; cycle++) sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 0 && FarmlandWaterManager.hasBlockWaterTicket(level, soil),
                "The final paid spray is watering even after the tank runs dry");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var valveHit = new BlockHitResult(Vec3.atCenterOf(valvePos), Direction.UP, valvePos, false);
        valve.useWithoutItem(level.getBlockState(valvePos), level, valvePos, player, valveHit);
        h.assertFalse(sprinkler.hasCurrentSpray() || FarmlandWaterManager.hasBlockWaterTicket(level, soil),
                "Closing the valve immediately pauses both effects");

        var closed = level.getBlockState(valvePos);
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(valvePos, closed.setValue(IrrigationValveBlock.OPEN, true), quiet);
        h.assertTrue(sprinkler.inspectionStatus().equals("Paid spray ready to resume") && !sprinkler.hasCurrentSpray(),
                "A quiet remote change leaves the paid window waiting for its next update");
        level.setBlock(valvePos, closed, quiet);

        valve.useWithoutItem(level.getBlockState(valvePos), level, valvePos, player, valveHit);
        h.assertTrue(sprinkler.water() == 0 && SprinklerGrowth.covers(level, soil.above())
                        && FarmlandWaterManager.hasBlockWaterTicket(level, soil),
                "Reopening by hand resumes the same spray without waiting for a tick or charging again");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.SPANNER.get()));
        player.setShiftKeyDown(true);
        var headHit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        SpannerItem.onRightClick(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, headHit));
        h.assertFalse(sprinkler.hasCurrentSpray(), "Switching to Requires redstone pauses an unpowered head");
        SpannerItem.onRightClick(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, headHit));
        h.assertTrue(sprinkler.water() == 0 && sprinkler.redstoneMode() == 2 && SprinklerGrowth.covers(level, soil.above())
                        && FarmlandWaterManager.hasBlockWaterTicket(level, soil), "Switching to Ignores redstone resumes the paid spray immediately");
        h.succeed();
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

    @GameTest(template = "empty")
    public static void redstoneResumesThePaidSprayAtTheHeadAndAdjacentValve(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 108, 4));
        var soil = pos.below(3);
        var valve = pos.east();
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        for (int cycle = 1; cycle < 40; cycle++) sprinkler.irrigateCycle();
        for (BlockPos power : new BlockPos[]{pos.west(), valve.east()}) {
            level.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            h.assertFalse(sprinkler.hasCurrentSpray() || FarmlandWaterManager.hasBlockWaterTicket(level, soil),
                    "Power immediately pauses the head or adjacent valve");
            level.removeBlock(power, false);
            h.assertTrue(sprinkler.water() == 0 && FarmlandWaterManager.hasBlockWaterTicket(level, soil)
                            && SprinklerGrowth.covers(level, soil.above()),
                    "Removing power resumes the same paid spray in the neighbor update");
        }
        sprinkler.configure(true);
        h.assertFalse(sprinkler.hasCurrentSpray(), "Requires power pauses while the input is off");
        level.setBlock(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(sprinkler.water() == 0 && SprinklerGrowth.covers(level, soil.above()),
                "In Requires power mode, restoring the signal also resumes the paid spray immediately");
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
            h.assertTrue(head.inspectionStatus().equals("Watering; random crop growth is disabled")
                            && FarmInspection.describe(level, vanilla, false, true)
                                    .contains("Sprinkler: watering; random crop growth is disabled")
                            && FarmlandWaterManager.hasBlockWaterTicket(level, vanilla.below()),
                    "No growth bonus is advertised when only hydration is active");
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
        h.assertTrue(FarmlandWaterManager.hasBlockWaterTicket(level, watered), "The first spray keeps its farmland hydrated");

        level.setBlock(added, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(sprinkler.wateredSoils().contains(added) && !SprinklerGrowth.covers(level, added.above()),
                "The preview can see new farmland before a spray waters it");
        h.assertTrue(sprinkler.inspectionStatus().contains("Watering 1/2 reachable plots"),
                "The report distinguishes watered and waiting plots in the same spray");
        level.setBlock(watered.above(2), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        var plots = SprinklerGrowth.coveredPlots(level);
        h.assertTrue(!SprinklerGrowth.covers(level, watered.above()) && !plots.contains(watered.above()) && !plots.contains(added.above()),
                "Blocked and newly added farmland both wait");
        h.assertFalse(FarmlandWaterManager.hasBlockWaterTicket(level, watered)
                || FarmlandWaterManager.hasBlockWaterTicket(level, added), "A new roof and new soil receive no hydration ticket");
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
        h.assertFalse(FarmlandWaterManager.hasBlockWaterTicket(level, soil), "A remote shutoff also immediately stops hydration");
        level.setBlock(valve, open, quietUpdate);
        h.assertTrue(SprinklerGrowth.covers(level, soil.above()) && SprinklerGrowth.coveredPlots(level).contains(soil.above())
                && FarmlandWaterManager.hasBlockWaterTicket(level, soil) && sprinkler.water() == 975,
                "Reopening before the next sprinkler tick restores the paid bonus and hydration");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void valveResumeOnlyWatersPreviouslyPaidPlots(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 84, 4));
        var watered = pos.south().below(3);
        var blocked = pos.west().below(3);
        var added = pos.north().below(3);
        var valve = pos.east();
        for (var soil : new BlockPos[] {watered, blocked}) {
            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(valve, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        var open = level.getBlockState(valve);
        level.setBlock(valve, open.setValue(IrrigationValveBlock.OPEN, false), Block.UPDATE_ALL);
        sprinkler.serverTick();
        h.assertFalse(level.getBlockState(pos).getValue(IrrigationSprinklerBlock.ACTIVE), "The shut valve pauses the spray");
        level.setBlock(blocked.above(2), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(added, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(valve, open, Block.UPDATE_ALL);
        sprinkler.serverTick();

        h.assertTrue(sprinkler.water() == 975 && SprinklerGrowth.covers(level, watered.above()),
                "An already watered plot can resume without another charge");
        h.assertFalse(SprinklerGrowth.covers(level, blocked.above()) || SprinklerGrowth.covers(level, added.above()),
                "A blocked plot and a new plot both wait for a new spray");
        h.assertTrue(level.getBlockState(added).getValue(FarmBlock.MOISTURE) == 0,
                "The resumed spray leaves the new plot dry");
        level.setBlock(blocked.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(SprinklerGrowth.covers(level, blocked.above()) && FarmlandWaterManager.hasBlockWaterTicket(level, blocked)
                        && !FarmlandWaterManager.hasBlockWaterTicket(level, added) && sprinkler.water() == 975,
                "An originally watered plot can recover mid-spray after the roof is removed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hydrationTicketFollowsSprinklerAcrossLoadedChunks(GameTestHelper h) {
        var level = h.getLevel();
        var chunk = new ChunkPos(h.absolutePos(new BlockPos(3328, 100, 4)));
        var pos = new BlockPos(chunk.getMaxBlockX(), 100, chunk.getMiddleBlockZ());
        var near = pos.west().below(2);
        var across = pos.east().below(2);
        level.getChunkAt(near);
        level.getChunkAt(across);
        level.setBlock(near, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(across, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        var sprinkler = head(h, pos);
        h.assertTrue(FarmlandWaterManager.hasBlockWaterTicket(level, near) && FarmlandWaterManager.hasBlockWaterTicket(level, across),
                "One spray covers farmland in both loaded chunks");
        sprinkler.onChunkUnloaded();
        h.assertFalse(FarmlandWaterManager.hasBlockWaterTicket(level, near) || FarmlandWaterManager.hasBlockWaterTicket(level, across),
                "Unloading the head clears hydration in both chunks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkReloadDropsThePreviousSpray(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(4, 90, 4));
        var soil = pos.south().below(3);
        level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        var sprinkler = head(h, pos);
        sprinkler.onChunkUnloaded();
        sprinkler.onLoad();
        h.assertFalse(sprinkler.hasCurrentSpray() || SprinklerGrowth.covers(level, soil.above()),
                "Chunk reload clears the old spray and its growth bonus");
        h.assertTrue(sprinkler.water() == 975, "The stored water survives the reload");
        if (level.getGameTime() % 40 != 0) {
            sprinkler.serverTick();
            h.assertFalse(sprinkler.hasCurrentSpray(), "The old interval cannot restart after a reload");
        }
        sprinkler.irrigateCycle();
        h.assertTrue(sprinkler.water() == 950 && SprinklerGrowth.covers(level, soil.above()),
                "A fresh paid spray works after reload");
        h.succeed();
    }
}
