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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class ValveSafetyGameTests {
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "valve-safety"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            var callback = hook;
            hook = null;
            if (callback != null) callback.run();
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(BlockPos pos, IrrigationValveBlockEntity valve, HookPlayer player) {}
    private static Rig rig(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 210, 4));
        h.getLevel().setBlock(pos, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState(), 18);
        var valve = (IrrigationValveBlockEntity) h.getLevel().getBlockEntity(pos);
        valve.rename("Garden valve");
        return new Rig(pos, valve, new HookPlayer(h.getLevel(), pos));
    }
    private static BlockHitResult hit(Rig rig) { return new BlockHitResult(Vec3.atCenterOf(rig.pos()), Direction.UP, rig.pos(), false); }
    private static void use(GameTestHelper h, Rig rig, InteractionHand hand) {
        AEBlocks.IRRIGATION_VALVE.get().useItemOn(rig.player().getItemInHand(hand), h.getLevel().getBlockState(rig.pos()), h.getLevel(), rig.pos(), rig.player(), hand, hit(rig));
    }
    private static IrrigationValveBlockEntity orphan(GameTestHelper h, Rig rig) {
        var valve = new IrrigationValveBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
        valve.setLevel(h.getLevel());
        var tag = new CompoundTag();
        tag.putString("Zone", "Saved zone");
        tag.putLong("Controller", rig.pos().above(3).asLong());
        valve.loadAdditional(tag, h.getLevel().registryAccess());
        return valve;
    }

    private static void interrupted(GameTestHelper h, int mutation) {
        var rig = rig(h);
        var hand = mutation % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var source = new ItemStack(Items.STICK, 3);
        rig.player().setItemInHand(hand, source);
        var expected = new ItemStack[]{source.copy()};
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.APPLE, 4));
                case 1 -> source.set(DataComponents.CUSTOM_NAME, Component.literal("Changed during access check"));
                case 2 -> source.grow(1);
                case 3 -> rig.player().setItemInHand(hand, source.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 7 -> {
                    h.getLevel().removeBlockEntity(rig.pos());
                    var current = new IrrigationValveBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
                    h.getLevel().setBlockEntity(current);
                    current.rename("Replacement valve");
                }
                default -> throw new AssertionError("Unknown callback mutation");
            }
            expected[0] = rig.player().getItemInHand(hand).copy();
        };
        use(h, rig, hand);
        h.assertTrue(h.getLevel().getBlockState(rig.pos()).getValue(IrrigationValveBlock.OPEN), "An interrupted click cannot toggle this valve or its replacement");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getItemInHand(hand)), "The callback's held item and saved data are preserved");
        if (mutation == 7) h.assertTrue(((IrrigationValveBlockEntity) h.getLevel().getBlockEntity(rig.pos())).zoneName().equals("Replacement valve"), "The replacement retains its own name");
        h.succeed();
    }
    @GameTest(template = "empty") public static void replacedValveToggleItemCancelsClick(GameTestHelper h) { interrupted(h, 0); }
    @GameTest(template = "empty") public static void changedValveToggleItemDataCancelsClick(GameTestHelper h) { interrupted(h, 1); }
    @GameTest(template = "empty") public static void changedValveToggleItemCountCancelsClick(GameTestHelper h) { interrupted(h, 2); }
    @GameTest(template = "empty") public static void matchingReplacementValveToggleItemCancelsClick(GameTestHelper h) { interrupted(h, 3); }
    @GameTest(template = "empty") public static void deathDuringValveToggleCancelsClick(GameTestHelper h) { interrupted(h, 4); }
    @GameTest(template = "empty") public static void spectatorChangeDuringValveToggleCancelsClick(GameTestHelper h) { interrupted(h, 5); }
    @GameTest(template = "empty") public static void dimensionChangeDuringValveToggleCancelsClick(GameTestHelper h) { interrupted(h, 6); }
    @GameTest(template = "empty") public static void replacementValveCannotInheritToggleClick(GameTestHelper h) { interrupted(h, 7); }

    private static void deadPlayer(GameTestHelper h, boolean empty) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, empty ? ItemStack.EMPTY : new ItemStack(Items.STICK, 3));
        rig.player().setHealth(0);
        if (empty) AEBlocks.IRRIGATION_VALVE.get().useWithoutItem(h.getLevel().getBlockState(rig.pos()), h.getLevel(), rig.pos(), rig.player(), hit(rig));
        else use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getBlockState(rig.pos()).getValue(IrrigationValveBlock.OPEN), "Dead players cannot toggle a valve with either an empty hand or an item");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deadPlayerCannotToggleValveWithItem(GameTestHelper h) { deadPlayer(h, false); }
    @GameTest(template = "empty") public static void deadPlayerCannotToggleValveWithEmptyHand(GameTestHelper h) { deadPlayer(h, true); }

    private static void staleUpdate(GameTestHelper h, boolean neighbor) {
        var rig = rig(h);
        var old = h.getLevel().getBlockState(rig.pos());
        h.getLevel().setBlock(rig.pos().east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        var current = h.getLevel().getBlockState(rig.pos()).setValue(IrrigationValveBlock.OPEN, false);
        h.getLevel().setBlock(rig.pos(), current, 18);
        var block = AEBlocks.IRRIGATION_VALVE.get();
        if (neighbor) block.neighborChanged(old, h.getLevel(), rig.pos(), AEBlocks.IRRIGATION_PIPE.get(), rig.pos().east(), false);
        else block.tick(old, h.getLevel(), rig.pos(), h.getLevel().random);
        h.assertTrue(h.getLevel().getBlockState(rig.pos()) == current && rig.valve().zoneName().equals("Garden valve"),
                "Old connection updates cannot reopen the closed valve or restore its previous fitting state");
        h.succeed();
    }
    @GameTest(template = "empty") public static void oldValveTickCannotReopenNewerClosedState(GameTestHelper h) { staleUpdate(h, false); }
    @GameTest(template = "empty") public static void oldValveNeighborUpdateCannotReopenClosedState(GameTestHelper h) { staleUpdate(h, true); }

    private static void staleMutation(GameTestHelper h, boolean removed, boolean rename) {
        var rig = rig(h);
        var old = removed ? rig.valve() : orphan(h, rig);
        String name = old.zoneName();
        var controller = old.controller();
        if (removed) old.setRemoved();
        if (rename) old.rename("Late rename"); else old.setController(rig.pos().above(5));
        h.assertTrue(old.zoneName().equals(name) && java.util.Objects.equals(old.controller(), controller), "An old valve reference cannot change its saved name or controller link");
        h.succeed();
    }
    @GameTest(template = "empty") public static void removedValveCannotRename(GameTestHelper h) { staleMutation(h, true, true); }
    @GameTest(template = "empty") public static void detachedValveCannotRename(GameTestHelper h) { staleMutation(h, false, true); }
    @GameTest(template = "empty") public static void removedValveCannotChangeController(GameTestHelper h) { staleMutation(h, true, false); }
    @GameTest(template = "empty") public static void detachedValveCannotChangeController(GameTestHelper h) { staleMutation(h, false, false); }

    @GameTest(template = "empty")
    public static void worldFreeValveControllerCleanupIsHarmless(GameTestHelper h) {
        var valve = new IrrigationValveBlockEntity(BlockPos.ZERO, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState());
        var tag = new CompoundTag();
        var controller = new BlockPos(2, 4, 6);
        tag.putLong("Controller", controller.asLong());
        valve.loadAdditional(tag, h.getLevel().registryAccess());
        valve.clearMissingController();
        h.assertTrue(controller.equals(valve.controller()), "A valve without a world retains its saved link until it is attached");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void worldFreeValveScheduleChangeIsHarmless(GameTestHelper h) {
        var valve = new IrrigationValveBlockEntity(BlockPos.ZERO, AEBlocks.IRRIGATION_VALVE.get().defaultBlockState());
        valve.setScheduledOpen(true);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedValveCleanupKeepsSavedController(GameTestHelper h) {
        var rig = rig(h);
        var controller = rig.pos().above(3);
        rig.valve().setController(controller);
        rig.valve().setRemoved();
        rig.valve().clearMissingController();
        h.assertTrue(controller.equals(rig.valve().controller()), "Late cleanup cannot erase the old valve's persisted link");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void valveControllerPositionIsCopied(GameTestHelper h) {
        var rig = rig(h);
        var original = rig.pos().above(3);
        var mutable = new BlockPos.MutableBlockPos(original.getX(), original.getY(), original.getZ());
        rig.valve().setController(mutable);
        mutable.move(Direction.EAST);
        h.assertTrue(original.equals(rig.valve().controller()), "Changing a caller's mutable position cannot silently move the saved controller link");
        h.succeed();
    }

    private static void oldLoad(GameTestHelper h, boolean removed) {
        var pos = h.absolutePos(new BlockPos(4, 205, 4));
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 18);
        var block = AEBlocks.IRRIGATION_VALVE.get();
        var old = new IrrigationValveBlockEntity(pos, block.defaultBlockState());
        old.setLevel(h.getLevel());
        if (removed) old.setRemoved();
        h.assertFalse(h.getLevel().getBlockTicks().hasScheduledTick(pos, block), "The replacement block has no valve work queued");
        old.onLoad();
        h.assertFalse(h.getLevel().getBlockTicks().hasScheduledTick(pos, block), "An old load callback cannot schedule repair work at a replacement block");
        h.succeed();
    }
    @GameTest(template = "empty") public static void removedValveLoadCannotScheduleRepair(GameTestHelper h) { oldLoad(h, true); }
    @GameTest(template = "empty") public static void detachedValveLoadCannotScheduleRepair(GameTestHelper h) { oldLoad(h, false); }

    @GameTest(template = "empty")
    public static void removedValveCannotAuthorizeScheduledFlow(GameTestHelper h) {
        var rig = rig(h);
        rig.valve().setRemoved();
        h.assertFalse(rig.valve().scheduleAllowsFlow(false) || rig.valve().scheduleAllowsFlow(true), "Removed valves cannot authorize ordinary or refill flow");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldOpenValveSnapshotCannotAuthorizeFlow(GameTestHelper h) {
        var rig = rig(h);
        var old = h.getLevel().getBlockState(rig.pos());
        h.getLevel().setBlock(rig.pos(), old.setValue(IrrigationValveBlock.OPEN, false), 18);
        h.assertFalse(IrrigationValveBlock.permitsFlow(h.getLevel(), rig.pos(), old), "A stale open snapshot cannot bypass a valve that is now closed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nonValveStateCannotAuthorizeFlow(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 205, 4));
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 18);
        h.assertFalse(IrrigationValveBlock.permitsFlow(h.getLevel(), pos, Blocks.STONE.defaultBlockState()), "Flow checks for a replacement block return unavailable safely");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentValveTogglesFromBothHands(GameTestHelper h) {
        var rig = rig(h);
        boolean open = true;
        for (var hand : InteractionHand.values()) {
            var held = new ItemStack(Items.STICK, 3);
            rig.player().setItemInHand(hand, held);
            use(h, rig, hand);
            open = !open;
            h.assertTrue(h.getLevel().getBlockState(rig.pos()).getValue(IrrigationValveBlock.OPEN) == open && held.getCount() == 3, "Each normal hand toggles the valve without spending the held item");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentEmptyHandValveToggleStillWorks(GameTestHelper h) {
        var rig = rig(h);
        AEBlocks.IRRIGATION_VALVE.get().useWithoutItem(h.getLevel().getBlockState(rig.pos()), h.getLevel(), rig.pos(), rig.player(), hit(rig));
        h.assertFalse(h.getLevel().getBlockState(rig.pos()).getValue(IrrigationValveBlock.OPEN), "A normal empty-hand click closes the valve");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentValveTickRepairsConnectionsAndRedstone(GameTestHelper h) {
        var rig = rig(h);
        var level = h.getLevel();
        level.setBlock(rig.pos().east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 18);
        var state = level.getBlockState(rig.pos()).setValue(IrrigationValveBlock.POWERED, true);
        level.setBlock(rig.pos(), state, 18);
        AEBlocks.IRRIGATION_VALVE.get().tick(state, level, rig.pos(), level.random);
        var current = level.getBlockState(rig.pos());
        h.assertTrue(!current.getValue(IrrigationValveBlock.POWERED) && current.getValue(IrrigationValveBlock.FACING).getAxis() == Direction.Axis.X
                        && current.getValue(IrrigationSprinklerBlock.connection(Direction.EAST))
                        && level.getBlockState(rig.pos().east()).getValue(IrrigationSprinklerBlock.connection(Direction.WEST)),
                "Current repair ticks still align both fittings and read the live redstone signal");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentValveNameKeepsUnicodeAndSaveData(GameTestHelper h) {
        var rig = rig(h);
        var expected = "🌱".repeat(32);
        rig.valve().rename("  " + expected + "extra  ");
        var tag = new CompoundTag();
        rig.valve().saveAdditional(tag, h.getLevel().registryAccess());
        var restored = new IrrigationValveBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
        restored.loadAdditional(tag, h.getLevel().registryAccess());
        h.assertTrue(rig.valve().zoneName().equals(expected) && restored.zoneName().equals(expected), "Names retain 32 complete Unicode code points across a save");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void currentControllerAndManualValveControlsStillAgree(GameTestHelper h) {
        var rig = rig(h);
        var level = h.getLevel();
        level.setBlock(rig.pos().east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        level.setBlock(rig.pos().east(2), AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, true), 3);
        var controllerPos = rig.pos().above(3);
        level.setBlock(controllerPos, AEBlocks.IRRIGATION_CONTROLLER.get().defaultBlockState(), 3);
        var controller = (IrrigationControllerBlockEntity) level.getBlockEntity(controllerPos);
        controller.link(rig.valve());
        controller.action(0, 2);
        h.assertTrue(controller.active() == 0 && rig.valve().scheduleAllowsFlow(false), "A live manual run opens the named linked zone");
        controller.action(0, 5);
        for (int i = 0; i < 2; i++) {
            use(h, rig, InteractionHand.MAIN_HAND);
            h.assertFalse(IrrigationValveBlock.permitsFlow(level, rig.pos(), level.getBlockState(rig.pos())), "Manual wheel toggles cannot bypass a disabled linked controller");
        }
        controller.release();
        h.assertTrue(rig.valve().controller() == null && rig.valve().scheduleAllowsFlow(false), "Releasing a current link restores ordinary manual flow");
        h.succeed();
    }
}
