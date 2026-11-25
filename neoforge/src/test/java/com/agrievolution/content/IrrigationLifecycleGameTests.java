package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationLifecycleGameTests {
    private static final int QUIET = 18;
    private record Head(BlockPos pos, BlockPos soil, IrrigationSprinklerBlockEntity entity) {}

    private static Head head(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 240, 4));
        var soil = pos.below(2);
        h.getLevel().setBlock(soil, Blocks.FARMLAND.defaultBlockState(), QUIET);
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), QUIET);
        return new Head(pos, soil, (IrrigationSprinklerBlockEntity) h.getLevel().getBlockEntity(pos));
    }

    private static IrrigationSprinklerBlockEntity detached(GameTestHelper h, Head head, int water) {
        var old = new IrrigationSprinklerBlockEntity(head.pos(), AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState());
        old.setLevel(h.getLevel());
        var tag = new CompoundTag();
        tag.putInt("SprinklerWater", water);
        old.loadAdditional(tag, h.getLevel().registryAccess());
        return old;
    }

    private static void atWateringTick(GameTestHelper h, Runnable test) {
        h.runAfterDelay(40 - h.getLevel().getGameTime() % 40, test);
    }

    private static void assertSpraying(GameTestHelper h, Head head) {
        h.assertTrue(h.getLevel().getBlockEntity(head.pos()) == head.entity()
                        && head.entity().water() == 975 && head.entity().growthActive()
                        && h.getLevel().getBlockState(head.pos()).getValue(IrrigationSprinklerBlock.ACTIVE)
                        && SprinklerGrowth.coveredPlots(h.getLevel()).contains(head.soil().above())
                        && FarmlandWaterManager.hasBlockWaterTicket(h.getLevel(), head.soil()),
                "The live replacement retains its exact water, active spray, growth registration and hydration ticket");
    }

    @GameTest(template = "empty")
    public static void removedSprinklerCannotChangeControls(GameTestHelper h) {
        var head = head(h);
        head.entity().setRemoved();
        head.entity().configure(false);
        head.entity().configure(true);
        h.assertTrue(!head.entity().smartWatering() && head.entity().redstoneMode() == 0, "Removed heads cannot change their stored controls");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerCannotChangeControls(GameTestHelper h) {
        var head = head(h);
        var old = detached(h, head, 1000);
        old.configure(false);
        old.configure(true);
        h.assertTrue(!old.smartWatering() && old.redstoneMode() == 0 && !head.entity().smartWatering(), "Only the placed head owns its settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerCannotSpendWaterOrHydrate(GameTestHelper h) {
        var head = head(h);
        var old = detached(h, head, 1000);
        old.irrigateCycle();
        h.assertTrue(old.water() == 1000 && head.entity().water() == 0
                        && !h.getLevel().getBlockState(head.pos()).getValue(IrrigationSprinklerBlock.ACTIVE)
                        && h.getLevel().getBlockState(head.soil()).getValue(FarmBlock.MOISTURE) == 0,
                "A detached watering cycle cannot spend stored water, activate the replacement or hydrate its farmland");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerCannotDrainConnectedBasin(GameTestHelper h) {
        var head = head(h);
        h.getLevel().setBlock(head.pos().west(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        var basin = head.pos().west(2);
        h.getLevel().setBlock(basin, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), 3);
        var old = detached(h, head, 0);
        old.irrigateCycle();
        h.assertTrue(old.water() == 0 && head.entity().water() == 0 && h.getLevel().getBlockState(basin).getValue(IrrigationBasinBlock.FILLED),
                "An old head cannot take a bucket from the live head's pipe network");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerCannotStartAfterFill(GameTestHelper h) {
        var head = head(h);
        var old = detached(h, head, 1000);
        old.startAfterFill();
        h.assertTrue(old.water() == 1000 && !h.getLevel().getBlockState(head.pos()).getValue(IrrigationSprinklerBlock.ACTIVE),
                "A delayed bucket-start callback leaves the old buffer and replacement alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void worldFreeSprinklerStartIsHarmless(GameTestHelper h) {
        var head = new IrrigationSprinklerBlockEntity(BlockPos.ZERO, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState());
        head.startAfterFill();
        h.assertTrue(head.water() == 0, "A head without a level cannot start a spray");
        h.succeed();
    }

    private static Head replacement(GameTestHelper h, Head old) {
        h.getLevel().removeBlockEntity(old.pos());
        h.getLevel().setBlock(old.pos().east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), QUIET);
        var current = new IrrigationSprinklerBlockEntity(old.pos(), h.getLevel().getBlockState(old.pos()));
        h.getLevel().setBlockEntity(current);
        h.assertTrue(current.fillBucket(), "The replacement accepts its own bucket");
        current.irrigateCycle();
        var head = new Head(old.pos(), old.soil(), current);
        assertSpraying(h, head);
        return head;
    }

    @GameTest(template = "empty")
    public static void removedSprinklerLoadCannotResetReplacement(GameTestHelper h) {
        var old = head(h);
        var current = replacement(h, old);
        old.entity().onLoad();
        assertSpraying(h, current);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerLoadCannotResetReplacement(GameTestHelper h) {
        var original = head(h);
        var old = detached(h, original, 0);
        var current = replacement(h, original);
        old.onLoad();
        assertSpraying(h, current);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedSprinklerTickCannotRepairOverReplacement(GameTestHelper h) {
        atWateringTick(h, () -> {
            var old = head(h);
            var current = replacement(h, old);
            old.entity().serverTick();
            assertSpraying(h, current);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void detachedSprinklerTickCannotSpendItsBuffer(GameTestHelper h) {
        atWateringTick(h, () -> {
            var head = head(h);
            var old = detached(h, head, 1000);
            old.serverTick();
            h.assertTrue(old.water() == 1000 && !h.getLevel().getBlockState(head.pos()).getValue(IrrigationSprinklerBlock.ACTIVE),
                    "A detached head cannot run a world-clock watering cycle");
            h.succeed();
        });
    }

    private static void lateCleanup(GameTestHelper h, boolean unload) {
        var old = head(h);
        var current = replacement(h, old);
        if (unload) old.entity().onChunkUnloaded(); else old.entity().setRemoved();
        assertSpraying(h, current);
        h.succeed();
    }

    @GameTest(template = "empty") public static void oldSprinklerRemovalKeepsReplacementGrowth(GameTestHelper h) { lateCleanup(h, false); }
    @GameTest(template = "empty") public static void oldSprinklerUnloadKeepsReplacementGrowth(GameTestHelper h) { lateCleanup(h, true); }

    private static void tickerContext(GameTestHelper h, int mismatch) {
        atWateringTick(h, () -> {
            var head = head(h);
            h.assertTrue(head.entity().fillBucket(), "The live head has one unused bucket");
            var block = AEBlocks.IRRIGATION_SPRINKLER.get();
            var state = h.getLevel().getBlockState(head.pos());
            var ticker = block.getTicker(h.getLevel(), state, AEBlockEntities.IRRIGATION_SPRINKLER.get());
            Level world = mismatch == 0 ? h.getLevel().getServer().getLevel(Level.NETHER) : h.getLevel();
            h.assertTrue(world != null && ticker != null, "Both server worlds and the sprinkler ticker are available");
            var pos = mismatch == 1 ? head.pos().above() : head.pos();
            var supplied = mismatch == 2 ? state.setValue(IrrigationSprinklerBlock.ACTIVE, true) : state;
            ticker.tick(world, pos, supplied, head.entity());
            if (mismatch == 3) assertSpraying(h, head);
            else h.assertTrue(head.entity().water() == 1000 && !head.entity().growthActive(), "A mismatched ticker context cannot charge or activate the live head");
            h.succeed();
        });
    }

    @GameTest(template = "empty") public static void sprinklerTickerRejectsWrongWorld(GameTestHelper h) { tickerContext(h, 0); }
    @GameTest(template = "empty") public static void sprinklerTickerRejectsWrongPosition(GameTestHelper h) { tickerContext(h, 1); }
    @GameTest(template = "empty") public static void sprinklerTickerRejectsOldState(GameTestHelper h) { tickerContext(h, 2); }
    @GameTest(template = "empty") public static void currentSprinklerTickerKeepsExactSprayCost(GameTestHelper h) { tickerContext(h, 3); }

    private static BlockState basinState(boolean filled, int tier) {
        return AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, filled).setValue(IrrigationBasinBlock.UPGRADE, tier);
    }

    private static void lateBasinLoad(GameTestHelper h, boolean removed) {
        var pos = h.absolutePos(new BlockPos(4, 225, 4));
        h.getLevel().setBlock(pos, basinState(false, 0), QUIET);
        var old = new IrrigationBasinBlockEntity(pos, basinState(true, 0));
        old.setLevel(h.getLevel());
        var legacy = new CompoundTag();
        legacy.putInt("IrrigationUpgradeTier", 2);
        old.loadAdditional(legacy, h.getLevel().registryAccess());
        if (removed) old.setRemoved();
        var current = h.getLevel().getBlockEntity(pos);
        old.onLoad();
        h.assertTrue(h.getLevel().getBlockEntity(pos) == current && h.getLevel().getBlockState(pos) == basinState(false, 0),
                "Late legacy migration cannot restore water or apply the old basin's tier to a replacement");
        h.succeed();
    }

    @GameTest(template = "empty") public static void removedBasinLoadCannotRestoreWaterAndTier(GameTestHelper h) { lateBasinLoad(h, true); }
    @GameTest(template = "empty") public static void detachedBasinLoadCannotRestoreWaterAndTier(GameTestHelper h) { lateBasinLoad(h, false); }

    @GameTest(template = "empty")
    public static void detachedBasinCannotRegisterFreeHydration(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 230, 4));
        var soil = pos.west(2);
        h.getLevel().setBlock(pos, basinState(false, 0), QUIET);
        h.getLevel().setBlock(soil, Blocks.FARMLAND.defaultBlockState(), QUIET);
        var old = new IrrigationBasinBlockEntity(pos, basinState(true, 2));
        old.setLevel(h.getLevel());
        old.refreshWatering();
        h.assertTrue(h.getLevel().getBlockState(soil).getValue(FarmBlock.MOISTURE) == 0, "A detached filled basin cannot hydrate farmland beside an empty replacement");
        old.setRemoved();
        h.succeed();
    }

    private static void basinContext(GameTestHelper h, int mismatch) {
        atWateringTick(h, () -> {
            var level = h.getLevel();
            var pos = h.absolutePos(new BlockPos(4, 220, 4));
            var state = basinState(false, 2);
            level.setBlock(pos, state, QUIET);
            level.setBlock(pos.west(), Blocks.WATER.defaultBlockState(), QUIET);
            var basin = (IrrigationBasinBlockEntity) level.getBlockEntity(pos);
            ServerLevel world = mismatch == 0 ? level.getServer().getLevel(Level.NETHER) : level;
            h.assertTrue(world != null, "The supplied server world is available");
            var target = mismatch == 1 ? pos.east(3) : pos;
            if (mismatch < 2) {
                world.getChunkAt(target);
                world.setBlock(target, state, QUIET);
            }
            if (mismatch == 2) {
                basin = new IrrigationBasinBlockEntity(pos, state);
                basin.setLevel(level);
            } else if (mismatch == 3) basin.setRemoved();
            IrrigationBasinBlockEntity.serverTick(world, target, state, basin);
            h.assertTrue(world.getBlockState(target).getValue(IrrigationBasinBlock.FILLED) == (mismatch == 4),
                    "Only the current basin and matching tick context can use its adjacent source");
            h.succeed();
        });
    }

    @GameTest(template = "empty") public static void basinTickerRejectsWrongWorld(GameTestHelper h) { basinContext(h, 0); }
    @GameTest(template = "empty") public static void basinTickerRejectsWrongPosition(GameTestHelper h) { basinContext(h, 1); }
    @GameTest(template = "empty") public static void detachedBasinCannotRefillReplacement(GameTestHelper h) { basinContext(h, 2); }
    @GameTest(template = "empty") public static void removedBasinTickCannotRefill(GameTestHelper h) { basinContext(h, 3); }
    @GameTest(template = "empty") public static void currentBasinTickRefillsWithoutConsumingSource(GameTestHelper h) { basinContext(h, 4); }

    @GameTest(template = "empty")
    public static void currentBasinLegacyMigrationStillWorks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 215, 4));
        h.getLevel().setBlock(pos, basinState(true, 0), QUIET);
        var basin = (IrrigationBasinBlockEntity) h.getLevel().getBlockEntity(pos);
        var legacy = new CompoundTag();
        legacy.putInt("IrrigationUpgradeTier", 1);
        basin.loadAdditional(legacy, h.getLevel().registryAccess());
        basin.onLoad();
        h.assertTrue(h.getLevel().getBlockState(pos) == basinState(true, 1) && basin.wateringRadius() == 8,
                "A current NBT-only copper upgrade retains its water and normal range");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentSprinklerLoadKeepsPaidSpray(GameTestHelper h) {
        var head = head(h);
        h.assertTrue(head.entity().fillBucket(), "Fill the live head");
        head.entity().startAfterFill();
        head.entity().onLoad();
        assertSpraying(h, head);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentSprinklerRemovalClearsGrowth(GameTestHelper h) {
        var head = head(h);
        h.assertTrue(head.entity().fillBucket(), "Fill the live head");
        head.entity().startAfterFill();
        h.getLevel().setBlock(head.pos(), Blocks.AIR.defaultBlockState(), QUIET);
        h.assertTrue(head.entity().isRemoved() && !head.entity().growthActive()
                        && !SprinklerGrowth.coveredPlots(h.getLevel()).contains(head.soil().above()),
                "Removing the actual head still releases its own growth registration");
        h.succeed();
    }
}
