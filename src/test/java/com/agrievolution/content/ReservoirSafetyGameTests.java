package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class ReservoirSafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger;
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "reservoir-safety"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (pos.equals(trigger) && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }

    private record Rig(BlockPos core, BlockPos panel, WaterReservoirBlockEntity tank, HookPlayer player) {}
    private static Rig rig(GameTestHelper h) {
        var core = h.absolutePos(new BlockPos(4, 190, 4));
        var tank = ReservoirGameTests.tank(h, core);
        return new Rig(core, core.west(), tank, new HookPlayer(h.getLevel(), core.west()));
    }
    private static ItemInteractionResult use(GameTestHelper h, Rig rig, InteractionHand hand, ItemStack stack, BlockState state) {
        return AEBlocks.WATER_RESERVOIR.get().useItemOn(stack, state, h.getLevel(), rig.panel(), rig.player(), hand,
                new BlockHitResult(Vec3.atCenterOf(rig.panel()), Direction.WEST, rig.panel(), false));
    }
    private static ItemInteractionResult use(GameTestHelper h, Rig rig, InteractionHand hand) {
        return use(h, rig, hand, rig.player().getItemInHand(hand), h.getLevel().getBlockState(rig.panel()));
    }
    private static CompoundTag saved(GameTestHelper h, WaterReservoirBlockEntity tank) {
        var tag = new CompoundTag();
        tank.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }

    private static void interrupted(GameTestHelper h, int action, boolean atCore, int mutation) {
        var rig = rig(h);
        var hand = action == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var source = new ItemStack(action == 0 ? Items.WATER_BUCKET : action == 1 ? Items.BUCKET : Items.CONDUIT, action == 0 ? 1 : 3);
        int originalCount = source.getCount();
        rig.player().setItemInHand(hand, source);
        rig.player().trigger = atCore ? rig.core() : rig.panel();
        var expected = new ItemStack[]{source.copy()};
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 3));
                case 1 -> source.set(DataComponents.CUSTOM_NAME, Component.literal("Changed during permission check"));
                case 2 -> source.grow(1);
                case 3 -> rig.player().setItemInHand(hand, source.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().getAbilities().mayBuild = false;
                case 7 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                default -> throw new AssertionError("Unknown callback mutation");
            }
            expected[0] = rig.player().getItemInHand(hand).copy();
        };
        var result = use(h, rig, hand);
        h.assertTrue(result == ItemInteractionResult.FAIL && rig.tank().water() == 1000 && !rig.tank().renewable(),
                "Interrupted bucket and upgrade actions cannot transfer water or install a module");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getItemInHand(hand)), "The callback's held item, count and saved data survive the cancelled action");
        if (mutation == 0 || mutation == 3) h.assertTrue(source.getCount() == originalCount, "A replaced source item is not charged through the old reference");
        h.succeed();
    }

    @GameTest(template = "empty") public static void changedReservoirFillBucketCancelsAtPanel(GameTestHelper h) { interrupted(h, 0, false, 0); }
    @GameTest(template = "empty") public static void changedReservoirDrainBucketCancelsAtCore(GameTestHelper h) { interrupted(h, 1, true, 0); }
    @GameTest(template = "empty") public static void changedReservoirConduitCancelsAtCore(GameTestHelper h) { interrupted(h, 2, true, 0); }
    @GameTest(template = "empty") public static void changedReservoirBucketDataCancelsTransfer(GameTestHelper h) { interrupted(h, 0, true, 1); }
    @GameTest(template = "empty") public static void changedReservoirConduitDataCancelsUpgrade(GameTestHelper h) { interrupted(h, 2, false, 1); }
    @GameTest(template = "empty") public static void changedReservoirBucketCountCancelsTransfer(GameTestHelper h) { interrupted(h, 0, false, 2); }
    @GameTest(template = "empty") public static void changedReservoirConduitCountCancelsUpgrade(GameTestHelper h) { interrupted(h, 2, true, 2); }
    @GameTest(template = "empty") public static void replacedMatchingReservoirBucketCancelsTransfer(GameTestHelper h) { interrupted(h, 0, true, 3); }
    @GameTest(template = "empty") public static void deathDuringReservoirDrainCancelsTransfer(GameTestHelper h) { interrupted(h, 1, true, 4); }
    @GameTest(template = "empty") public static void spectatorChangeDuringReservoirUpgradeCancels(GameTestHelper h) { interrupted(h, 2, true, 5); }
    @GameTest(template = "empty") public static void revokedBuildPermissionCancelsReservoirFill(GameTestHelper h) { interrupted(h, 0, true, 6); }
    @GameTest(template = "empty") public static void dimensionChangeCancelsReservoirFill(GameTestHelper h) { interrupted(h, 0, true, 7); }

    @GameTest(template = "empty")
    public static void reservoirCannotUseAnItemFromTheWrongHand(GameTestHelper h) {
        var rig = rig(h);
        var bucket = new ItemStack(Items.WATER_BUCKET);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, bucket);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 4));
        use(h, rig, InteractionHand.OFF_HAND, bucket, h.getLevel().getBlockState(rig.panel()));
        h.assertTrue(rig.tank().water() == 1000 && bucket.is(Items.WATER_BUCKET) && rig.player().getOffhandItem().is(Items.STICK)
                        && rig.player().getOffhandItem().getCount() == 4, "An old hand argument cannot charge another hand's item or replace its contents");
        h.succeed();
    }

    private static void deadPlayer(GameTestHelper h, boolean upgrade) {
        var rig = rig(h);
        var item = new ItemStack(upgrade ? Items.CONDUIT : Items.WATER_BUCKET, upgrade ? 2 : 1);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, item);
        rig.player().setHealth(0);
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(rig.tank().water() == 1000 && !rig.tank().renewable() && rig.player().getMainHandItem() == item && item.getCount() == (upgrade ? 2 : 1),
                "Dead players cannot transfer water or install reservoir upgrades");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deadPlayerCannotFillReservoir(GameTestHelper h) { deadPlayer(h, false); }
    @GameTest(template = "empty") public static void deadPlayerCannotUpgradeReservoir(GameTestHelper h) { deadPlayer(h, true); }

    private static void replacedTank(GameTestHelper h, boolean atCore) {
        var rig = rig(h);
        var tag = saved(h, rig.tank());
        var replacement = new WaterReservoirBlockEntity(rig.core(), rig.tank().getBlockState());
        replacement.loadAdditional(tag, h.getLevel().registryAccess());
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        rig.player().trigger = atCore ? rig.core() : rig.panel();
        rig.player().hook = () -> {
            h.getLevel().removeBlockEntity(rig.core());
            h.getLevel().setBlockEntity(replacement);
        };
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getBlockEntity(rig.core()) == replacement && replacement.water() == 1000 && rig.tank().water() == 1000
                        && rig.player().getMainHandItem().is(Items.WATER_BUCKET), "Replacing the core during either permission check cancels the old fill and preserves both tanks");
        h.succeed();
    }
    @GameTest(template = "empty") public static void reservoirPanelCallbackCannotFillReplacementCore(GameTestHelper h) { replacedTank(h, false); }
    @GameTest(template = "empty") public static void reservoirCoreCallbackCannotFillReplacementCore(GameTestHelper h) { replacedTank(h, true); }

    @GameTest(template = "empty")
    public static void oldReservoirPanelCoordinatesCannotRetargetClick(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var old = h.getLevel().getBlockState(rig.panel()).setValue(WaterReservoirBlock.X, 1);
        use(h, rig, InteractionHand.MAIN_HAND, rig.player().getMainHandItem(), old);
        h.assertTrue(rig.tank().water() == 1000 && rig.player().getMainHandItem().is(Items.WATER_BUCKET), "An old panel location cannot redirect a bucket to the panel's newer core");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldReservoirGaugeSnapshotStillAllowsBucketUse(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var old = h.getLevel().getBlockState(rig.panel()).setValue(WaterReservoirBlock.FILL, 4);
        use(h, rig, InteractionHand.MAIN_HAND, rig.player().getMainHandItem(), old);
        h.assertTrue(rig.tank().water() == 2000 && rig.player().getMainHandItem().is(Items.BUCKET), "Gauge-only differences still resolve the same authoritative tank");
        h.succeed();
    }

    private static void nearGeneration(GameTestHelper h, WaterReservoirBlockEntity tank, int water) {
        var tag = saved(h, tank);
        tag.putInt("Water", water);
        tag.putBoolean("Renewable", true);
        tag.putInt("GenerationSeconds", 9);
        tank.loadAdditional(tag, h.getLevel().registryAccess());
    }
    private static void generationUnchanged(GameTestHelper h, WaterReservoirBlockEntity tank, int water) {
        h.assertTrue(tank.water() == water && saved(h, tank).getInt("GenerationSeconds") == 9,
                "A rejected generation call preserves both stored water and the partial generation cycle");
    }

    @GameTest(template = "empty")
    public static void staleReservoirCoreTickCannotAdvanceGeneration(GameTestHelper h) {
        var rig = rig(h);
        nearGeneration(h, rig.tank(), 1000);
        var block = AEBlocks.WATER_RESERVOIR.get();
        var current = h.getLevel().getBlockState(rig.core());
        block.tick(current.setValue(WaterReservoirBlock.FILL, 0), h.getLevel(), rig.core(), h.getLevel().random);
        generationUnchanged(h, rig.tank(), 1000);
        block.tick(current, h.getLevel(), rig.core(), h.getLevel().random);
        h.assertTrue(rig.tank().water() == 2000 && saved(h, rig.tank()).getInt("GenerationSeconds") == 0, "The next current core tick generates exactly one bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldReservoirGaugeCannotOverwriteNewPanelState(GameTestHelper h) {
        var rig = rig(h);
        var old = h.getLevel().getBlockState(rig.panel());
        var current = old.setValue(WaterReservoirBlock.FILL, 3);
        h.getLevel().setBlock(rig.panel(), current, 18);
        h.assertFalse(WaterReservoirBlock.refreshGauge(h.getLevel(), rig.panel(), old, 2), "An old gauge update cannot commit over a newer state");
        h.assertTrue(h.getLevel().getBlockState(rig.panel()) == current && rig.tank().water() == 1000, "The new panel and authoritative water remain unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldReservoirGaugeCannotRestoreRemovedPanel(GameTestHelper h) {
        var rig = rig(h);
        var old = h.getLevel().getBlockState(rig.panel());
        h.getLevel().setBlock(rig.panel(), Blocks.STONE.defaultBlockState(), 18);
        h.assertFalse(WaterReservoirBlock.refreshGauge(h.getLevel(), rig.panel(), old, 2), "A late gauge update cannot restore a dismantled shell part");
        h.assertTrue(h.getLevel().getBlockState(rig.panel()).is(Blocks.STONE), "The replacement block survives");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedReservoirCannotGenerateThroughOldHelper(GameTestHelper h) {
        var rig = rig(h);
        nearGeneration(h, rig.tank(), 1000);
        rig.tank().setRemoved();
        rig.tank().generateSecondInCompleteTank();
        generationUnchanged(h, rig.tank(), 1000);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedReservoirCannotGenerateOrChangeLiveGauges(GameTestHelper h) {
        var rig = rig(h);
        var old = new WaterReservoirBlockEntity(rig.core(), rig.tank().getBlockState());
        old.setLevel(h.getLevel());
        nearGeneration(h, old, 8000);
        old.generateSecondInCompleteTank();
        generationUnchanged(h, old, 8000);
        h.assertTrue(rig.tank().water() == 1000 && h.getLevel().getBlockState(rig.panel()).getValue(WaterReservoirBlock.FILL) == 1,
                "Detached generation cannot alter a placed tank or its shell's water gauge");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void incompleteReservoirCannotUseGenerationHelper(GameTestHelper h) {
        var rig = rig(h);
        nearGeneration(h, rig.tank(), 1000);
        var corner = rig.core().offset(-1, 1, -1);
        h.getLevel().setBlock(corner, h.getLevel().getBlockState(corner).setValue(WaterReservoirBlock.X, 1), 18);
        h.assertFalse(rig.tank().complete(), "The changed corner makes this shell incomplete");
        rig.tank().generateSecondInCompleteTank();
        generationUnchanged(h, rig.tank(), 1000);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void worldFreeReservoirCannotGenerate(GameTestHelper h) {
        var tank = new WaterReservoirBlockEntity(BlockPos.ZERO, AEBlocks.WATER_RESERVOIR.get().defaultBlockState());
        nearGeneration(h, tank, 8000);
        tank.generateSecondInCompleteTank();
        generationUnchanged(h, tank, 8000);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirBucketsKeepExactCostsInBothHands(GameTestHelper h) {
        var rig = rig(h);
        for (var hand : InteractionHand.values()) {
            rig.player().setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            use(h, rig, hand);
            h.assertTrue(rig.tank().water() == 2000 && rig.player().getItemInHand(hand).is(Items.BUCKET), "Filling spends one water bucket in the used hand");
            rig.player().setItemInHand(hand, new ItemStack(Items.BUCKET, 3));
            use(h, rig, hand);
            h.assertTrue(rig.tank().water() == 1000 && rig.player().getItemInHand(hand).is(Items.BUCKET) && rig.player().getItemInHand(hand).getCount() == 2,
                    "Draining spends one empty bucket and retains the rest of the stack");
        }
        h.assertTrue(rig.player().getInventory().countItem(Items.WATER_BUCKET) == 2, "Both collected buckets reach the player inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirOffhandUpgradeConsumesOneConduit(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CONDUIT, 3));
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(rig.tank().renewable() && rig.tank().water() == 1000 && rig.player().getOffhandItem().getCount() == 2, "A normal offhand upgrade installs Mk II and spends exactly one Conduit");
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(rig.player().getOffhandItem().getCount() == 2, "An already-upgraded tank does not consume another Conduit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirGaugeChangeDuringPermissionStillAllowsFill(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        rig.player().trigger = rig.core();
        rig.player().hook = () -> h.getLevel().setBlock(rig.panel(), h.getLevel().getBlockState(rig.panel()).setValue(WaterReservoirBlock.FILL, 4), 18);
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(rig.tank().water() == 2000 && rig.player().getMainHandItem().is(Items.BUCKET), "A gauge-only update does not cancel a valid fill into the same core");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reservoirGenerationKeepsPartialCycleAcrossSave(GameTestHelper h) {
        var rig = rig(h);
        h.assertTrue(rig.tank().upgrade(), "Install Mk II");
        for (int i = 0; i < 5; i++) rig.tank().generationSecond();
        rig.tank().loadAdditional(saved(h, rig.tank()), h.getLevel().registryAccess());
        for (int i = 0; i < 4; i++) rig.tank().generationSecond();
        h.assertTrue(rig.tank().water() == 1000, "Nine saved and current seconds do not produce an early bucket");
        rig.tank().generationSecond();
        h.assertTrue(rig.tank().water() == 2000, "The tenth second after reload produces exactly 1000 mB");
        h.succeed();
    }
}
