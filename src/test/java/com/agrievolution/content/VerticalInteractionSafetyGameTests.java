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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class VerticalInteractionSafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger;
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "column-bucket"));
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

    private record Rig(BlockPos base, VerticalGreenhouseBlockEntity entity, HookPlayer player) {}
    private static Rig rig(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(4, 210, 4));
        var block = AEBlocks.VERTICAL_GREENHOUSE.get();
        var state = block.defaultBlockState().setValue(VerticalGreenhouseBlock.FACING, Direction.NORTH);
        h.getLevel().setBlock(base, state, 3);
        block.setPlacedBy(h.getLevel(), base, state, null, new ItemStack(AEItems.VERTICAL_GREENHOUSE.get()));
        var entity = (VerticalGreenhouseBlockEntity) h.getLevel().getBlockEntity(base);
        h.assertTrue(entity != null && entity.complete() && entity.addBucket(), "The fixture has a complete column and one stored bucket");
        entity.setItem(0, new ItemStack(AEItems.TOMATO_SEED.get()));
        return new Rig(base, entity, new HookPlayer(h.getLevel(), base.above(2)));
    }
    private static CompoundTag saved(GameTestHelper h, VerticalGreenhouseBlockEntity entity) {
        var tag = new CompoundTag();
        entity.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static ItemInteractionResult use(GameTestHelper h, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        return AEBlocks.VERTICAL_GREENHOUSE.get().useItemOn(stack, state, h.getLevel(), pos, player, hand,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static ItemInteractionResult use(GameTestHelper h, Rig rig, int section, Player player, InteractionHand hand) {
        var pos = rig.base().above(section);
        return use(h, pos, h.getLevel().getBlockState(pos), player, hand, player.getItemInHand(hand));
    }
    private static void replace(GameTestHelper h, Rig rig) {
        var tag = saved(h, rig.entity());
        h.getLevel().removeBlockEntity(rig.base());
        var replacement = new VerticalGreenhouseBlockEntity(rig.base(), h.getLevel().getBlockState(rig.base()));
        replacement.loadAdditional(tag, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(replacement);
    }
    private static void interrupted(GameTestHelper h, boolean atBase, int mutation) {
        var rig = rig(h);
        var hand = atBase ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var source = new ItemStack(Items.WATER_BUCKET);
        rig.player().setItemInHand(hand, source);
        var expected = new ItemStack[]{source.copy()};
        var before = saved(h, rig.entity());
        rig.player().trigger = atBase ? rig.base() : rig.base().above(2);
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 3));
                case 1 -> source.set(DataComponents.CUSTOM_NAME, Component.literal("New bucket data"));
                case 2 -> source.grow(1);
                case 3 -> rig.player().setItemInHand(hand, source.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().getAbilities().mayBuild = false;
                case 7 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 8 -> replace(h, rig);
                case 9 -> {
                    for (int section = 0; section < 3; section++) {
                        var part = rig.base().above(section);
                        h.getLevel().setBlock(part, h.getLevel().getBlockState(part).setValue(VerticalGreenhouseBlock.FACING, Direction.EAST), 18);
                    }
                }
                default -> throw new AssertionError("Unknown bucket interruption");
            }
            expected[0] = rig.player().getItemInHand(hand).copy();
        };
        use(h, rig, 2, rig.player(), hand);
        h.assertTrue(rig.player().hook == null, "The requested section or base permission callback ran");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getItemInHand(hand)) && rig.player().getInventory().countItem(Items.BUCKET) == 0,
                "An interrupted fill keeps the newer held item and creates no empty bucket");
        h.assertTrue(before.equals(saved(h, rig.entity())), "An interrupted fill preserves the original tank, seeds and processing state");
        var current = (VerticalGreenhouseBlockEntity) h.getLevel().getBlockEntity(rig.base());
        h.assertTrue(current.water() == 1000, "A replacement tank cannot inherit a pending bucket fill");
        if (mutation == 9) h.assertTrue(current.complete() && current.getBlockState().getValue(VerticalGreenhouseBlock.FACING) == Direction.EAST,
                "The newer complete column orientation is retained");
        h.succeed();
    }

    @GameTest(template = "empty") public static void swappedBucketAtSectionCancelsColumnFill(GameTestHelper h) { interrupted(h, false, 0); }
    @GameTest(template = "empty") public static void renamedBucketAtSectionCancelsColumnFill(GameTestHelper h) { interrupted(h, false, 1); }
    @GameTest(template = "empty") public static void changedBucketCountAtSectionCancelsColumnFill(GameTestHelper h) { interrupted(h, false, 2); }
    @GameTest(template = "empty") public static void matchingBucketReplacementAtSectionCancelsColumnFill(GameTestHelper h) { interrupted(h, false, 3); }
    @GameTest(template = "empty") public static void swappedBucketAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 0); }
    @GameTest(template = "empty") public static void renamedBucketAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 1); }
    @GameTest(template = "empty") public static void changedBucketCountAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 2); }
    @GameTest(template = "empty") public static void matchingBucketReplacementAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 3); }
    @GameTest(template = "empty") public static void deathAtSectionCancelsColumnFill(GameTestHelper h) { interrupted(h, false, 4); }
    @GameTest(template = "empty") public static void deathAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 4); }
    @GameTest(template = "empty") public static void spectatorAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 5); }
    @GameTest(template = "empty") public static void revokedBuildAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 6); }
    @GameTest(template = "empty") public static void dimensionChangeAtBaseCancelsColumnFill(GameTestHelper h) { interrupted(h, true, 7); }
    @GameTest(template = "empty") public static void replacedTankAtSectionCannotInheritColumnFill(GameTestHelper h) { interrupted(h, false, 8); }
    @GameTest(template = "empty") public static void replacedTankAtBaseCannotInheritColumnFill(GameTestHelper h) { interrupted(h, true, 8); }
    @GameTest(template = "empty") public static void rotatedColumnAtBaseCancelsFill(GameTestHelper h) { interrupted(h, true, 9); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotFillColumn(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        rig.player().setHealth(0);
        use(h, rig, 0, rig.player(), InteractionHand.MAIN_HAND);
        h.assertTrue(rig.entity().water() == 1000 && rig.player().getMainHandItem().is(Items.WATER_BUCKET), "A dead player's bucket cannot fill any section");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void detachedStackCannotFillColumnThroughAnotherHand(GameTestHelper h) {
        var rig = rig(h);
        var detached = new ItemStack(Items.WATER_BUCKET);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, detached);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        var pos = rig.base().above();
        use(h, pos, h.getLevel().getBlockState(pos), rig.player(), InteractionHand.OFF_HAND, detached);
        h.assertTrue(rig.entity().water() == 1000 && rig.player().getOffhandItem().is(Items.STICK) && !detached.isEmpty(),
                "A stack outside the specified hand cannot fill the column or replace that hand");
        h.succeed();
    }

    private static void staleLookup(GameTestHelper h, int mutation) {
        var rig = rig(h);
        var pos = rig.base().above(2);
        var state = h.getLevel().getBlockState(pos);
        switch (mutation) {
            case 0 -> h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            case 1 -> h.getLevel().setBlock(pos, state.setValue(VerticalGreenhouseBlock.FACING, Direction.EAST), 18);
            default -> state = Blocks.STONE.defaultBlockState();
        }
        h.assertTrue(VerticalGreenhouseBlock.controller(h.getLevel(), pos, state) == null,
                "Controller lookup rejects old section snapshots and unrelated blocks");
        h.succeed();
    }
    @GameTest(template = "empty") public static void removedSectionCannotResolveColumnTank(GameTestHelper h) { staleLookup(h, 0); }
    @GameTest(template = "empty") public static void rotatedSectionSnapshotCannotResolveColumnTank(GameTestHelper h) { staleLookup(h, 1); }
    @GameTest(template = "empty") public static void foreignBlockSnapshotCannotResolveColumnTank(GameTestHelper h) { staleLookup(h, 3); }

    @GameTest(template = "empty")
    public static void incompleteColumnStillResolvesCurrentBaseForRepairs(GameTestHelper h) {
        var rig = rig(h);
        var upper = rig.base().above(2);
        h.getLevel().setBlock(upper, h.getLevel().getBlockState(upper).setValue(VerticalGreenhouseBlock.FACING, Direction.EAST), 18);
        h.assertTrue(!rig.entity().complete() && VerticalGreenhouseBlock.controller(h.getLevel(), rig.base(), h.getLevel().getBlockState(rig.base())) == rig.entity(),
                "The current base remains accessible for diagnosing an incomplete column");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void columnFillsFromEverySectionInEitherHand(GameTestHelper h) {
        var rig = rig(h);
        for (int section = 0; section < 3; section++) for (var hand : InteractionHand.values()) {
            rig.player().setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
            int before = rig.entity().water();
            use(h, rig, section, rig.player(), hand);
            h.assertTrue(rig.entity().water() == before + 1000 && rig.player().getItemInHand(hand).is(Items.BUCKET),
                    "Each section accepts one whole bucket and returns one empty bucket in the used hand");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeColumnFillDoesNotCreateEmptyBuckets(GameTestHelper h) {
        var rig = rig(h);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(rig.base()));
        for (int section = 0; section < 3; section++) for (var hand : InteractionHand.values()) {
            var held = new ItemStack(Items.WATER_BUCKET);
            held.set(DataComponents.CUSTOM_NAME, Component.literal("Creative supply"));
            player.setItemInHand(hand, held);
            var before = held.copy();
            int water = rig.entity().water();
            use(h, rig, section, player, hand);
            h.assertTrue(rig.entity().water() == water + 1000 && ItemStack.matches(before, player.getItemInHand(hand))
                            && player.getInventory().countItem(Items.BUCKET) == 0,
                    "Creative fill retains the named supply and creates no extra empty containers");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void stackedColumnWaterBucketsKeepSparesInEitherHand(GameTestHelper h) {
        var rig = rig(h);
        for (var hand : InteractionHand.values()) {
            rig.player().getInventory().clearContent();
            rig.player().setItemInHand(hand, new ItemStack(Items.WATER_BUCKET, 3));
            int before = rig.entity().water();
            use(h, rig, 1, rig.player(), hand);
            h.assertTrue(rig.entity().water() == before + 1000 && rig.player().getItemInHand(hand).is(Items.WATER_BUCKET)
                            && rig.player().getItemInHand(hand).getCount() == 2 && rig.player().getInventory().countItem(Items.BUCKET) == 1,
                    "One filled bucket is spent, with the spare stack retained and exactly one empty returned");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void columnBucketOverflowDropsOneReturnedContainer(GameTestHelper h) {
        var rig = rig(h);
        for (int slot = 0; slot < 36; slot++) rig.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET, 2));
        use(h, rig, 2, rig.player(), InteractionHand.OFF_HAND);
        int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(rig.base()).inflate(4)).stream()
                .filter(entity -> entity.getItem().is(Items.BUCKET)).mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(rig.entity().water() == 2000 && rig.player().getOffhandItem().is(Items.WATER_BUCKET)
                        && rig.player().getOffhandItem().getCount() == 1 && dropped == 1,
                "A full inventory drops exactly one returned empty bucket and retains the spare water bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void columnRejectsBucketsWithoutSpaceForTheWholeAmount(GameTestHelper h) {
        var rig = rig(h);
        h.assertTrue(rig.entity().fill(new FluidStack(Fluids.WATER, 14500), FluidAction.EXECUTE) == 14500, "Prepare only 500 mB of free space");
        for (int section = 0; section < 3; section++) {
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            use(h, rig, section, rig.player(), InteractionHand.MAIN_HAND);
            h.assertTrue(rig.entity().water() == 15500 && rig.player().getMainHandItem().is(Items.WATER_BUCKET),
                    "A bucket never partially fills or disappears when it cannot fit");
        }
        h.assertTrue(rig.entity().fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE) == 500, "Fill the remaining space");
        use(h, rig, 0, rig.player(), InteractionHand.MAIN_HAND);
        h.assertTrue(rig.entity().water() == 16000 && rig.player().getMainHandItem().is(Items.WATER_BUCKET), "A full tank keeps the bucket unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyMainHandPassesColumnFillsToOffhand(GameTestHelper h) {
        var rig = rig(h);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (int section = 0; section < 3; section++) {
            rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(use(h, rig, section, rig.player(), InteractionHand.MAIN_HAND) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "An empty main hand allows the offhand water bucket to handle each section");
            use(h, rig, section, rig.player(), InteractionHand.OFF_HAND);
        }
        h.assertTrue(rig.entity().water() == 4000 && rig.player().getOffhandItem().is(Items.BUCKET), "Each offhand bucket fills exactly once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void incompleteColumnKeepsManualWaterBucket(GameTestHelper h) {
        var rig = rig(h);
        h.getLevel().setBlock(rig.base().above(2), h.getLevel().getBlockState(rig.base().above(2)).setValue(VerticalGreenhouseBlock.FACING, Direction.EAST), 18);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        use(h, rig, 1, rig.player(), InteractionHand.MAIN_HAND);
        h.assertTrue(!rig.entity().complete() && rig.entity().water() == 1000 && rig.player().getMainHandItem().is(Items.WATER_BUCKET),
                "A mismatched upper section blocks manual filling without losing stored water");
        h.succeed();
    }
}
