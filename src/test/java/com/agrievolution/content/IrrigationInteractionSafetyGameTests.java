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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class IrrigationInteractionSafetyGameTests {
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "irrigation-click"));
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

    private record Rig(int kind, BlockPos pos, BlockEntity entity, HookPlayer player) {}
    private static Rig rig(GameTestHelper h, int kind, boolean draining) {
        var pos = h.absolutePos(new BlockPos(4, 180 + kind * 10, 4));
        var level = h.getLevel();
        if (kind == 0) {
            level.setBlock(pos, AEBlocks.IRRIGATION_SPRINKLER.get().defaultBlockState(), 18);
            h.assertTrue(((IrrigationSprinklerBlockEntity) level.getBlockEntity(pos)).fillBucket(), "The head starts with one stored bucket and no paid spray");
        } else {
            level.setBlock(pos, AEBlocks.IRRIGATION_BASIN.get().defaultBlockState().setValue(IrrigationBasinBlock.FILLED, draining || kind == 2), 18);
        }
        var player = new HookPlayer(level, pos);
        player.setShiftKeyDown(kind == 2);
        return new Rig(kind, pos, level.getBlockEntity(pos), player);
    }
    private static int water(GameTestHelper h, Rig rig) {
        return rig.kind() == 0 ? ((IrrigationSprinklerBlockEntity) h.getLevel().getBlockEntity(rig.pos())).water()
                : h.getLevel().getBlockState(rig.pos()).getValue(IrrigationBasinBlock.FILLED) ? 1000 : 0;
    }
    private static BlockHitResult hit(Rig rig) {
        return new BlockHitResult(Vec3.atCenterOf(rig.pos()), Direction.UP, rig.pos(), false);
    }
    private static void use(GameTestHelper h, Rig rig, InteractionHand hand) {
        var stack = rig.player().getItemInHand(hand);
        var state = h.getLevel().getBlockState(rig.pos());
        if (rig.kind() == 0) AEBlocks.IRRIGATION_SPRINKLER.get().useItemOn(stack, state, h.getLevel(), rig.pos(), rig.player(), hand, hit(rig));
        else if (rig.kind() == 1) AEBlocks.IRRIGATION_BASIN.get().useItemOn(stack, state, h.getLevel(), rig.pos(), rig.player(), hand, hit(rig));
        else IrrigationBasinUpgradeHandler.onRightClick(new PlayerInteractEvent.RightClickBlock(rig.player(), hand, rig.pos(), hit(rig)));
    }
    private static void replace(GameTestHelper h, Rig rig) {
        var state = h.getLevel().getBlockState(rig.pos());
        BlockEntity replacement;
        if (rig.kind() == 0) {
            var tag = new CompoundTag();
            ((IrrigationSprinklerBlockEntity) rig.entity()).saveAdditional(tag, h.getLevel().registryAccess());
            var head = new IrrigationSprinklerBlockEntity(rig.pos(), state);
            head.loadAdditional(tag, h.getLevel().registryAccess());
            replacement = head;
        } else replacement = new IrrigationBasinBlockEntity(rig.pos(), state);
        h.getLevel().removeBlockEntity(rig.pos());
        h.getLevel().setBlockEntity(replacement);
    }

    private static void interrupted(GameTestHelper h, int kind, boolean draining, int mutation, InteractionHand hand) {
        var rig = rig(h, kind, draining);
        int beforeWater = water(h, rig);
        var source = new ItemStack(kind == 2 ? Items.COPPER_BLOCK : draining ? Items.BUCKET : Items.WATER_BUCKET, kind == 2 || draining ? 3 : 1);
        int originalCount = source.getCount();
        rig.player().setItemInHand(hand, source);
        var expected = new ItemStack[]{source.copy()};
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 4));
                case 1 -> source.set(DataComponents.CUSTOM_NAME, Component.literal("Changed during access check"));
                case 2 -> source.grow(1);
                case 3 -> rig.player().setItemInHand(hand, source.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 7 -> rig.player().getAbilities().mayBuild = false;
                case 8 -> rig.player().setShiftKeyDown(false);
                case 9 -> replace(h, rig);
                default -> throw new AssertionError("Unknown callback mutation");
            }
            expected[0] = rig.player().getItemInHand(hand).copy();
        };
        use(h, rig, hand);
        h.assertTrue(water(h, rig) == beforeWater && (kind == 0 || ((IrrigationBasinBlockEntity) h.getLevel().getBlockEntity(rig.pos())).upgradeTier() == 0),
                "An interrupted click cannot transfer water, change a replacement or install a basin upgrade");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getItemInHand(hand)), "The callback's item, count and saved data are preserved");
        if (mutation == 0 || mutation == 3) h.assertTrue(source.getCount() == originalCount, "A replaced source stack is not charged through its old reference");
        h.succeed();
    }

    @GameTest(template = "empty") public static void changedSprinklerFillBucketCancels(GameTestHelper h) { interrupted(h, 0, false, 0, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void changedSprinklerDrainBucketCancels(GameTestHelper h) { interrupted(h, 0, true, 0, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void changedSprinklerBucketDataCancels(GameTestHelper h) { interrupted(h, 0, false, 1, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void changedSprinklerBucketCountCancels(GameTestHelper h) { interrupted(h, 0, true, 2, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void matchingReplacementSprinklerBucketCancels(GameTestHelper h) { interrupted(h, 0, false, 3, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void replacementSprinklerCannotInheritBucketClick(GameTestHelper h) { interrupted(h, 0, false, 9, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void deathDuringSprinklerFillCancels(GameTestHelper h) { interrupted(h, 0, false, 4, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void spectatorChangeDuringSprinklerDrainCancels(GameTestHelper h) { interrupted(h, 0, true, 5, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void dimensionChangeDuringSprinklerFillCancels(GameTestHelper h) { interrupted(h, 0, false, 6, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void changedBasinFillBucketCancels(GameTestHelper h) { interrupted(h, 1, false, 0, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void changedBasinDrainBucketCancels(GameTestHelper h) { interrupted(h, 1, true, 0, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void changedBasinBucketDataCancels(GameTestHelper h) { interrupted(h, 1, false, 1, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void changedBasinBucketCountCancels(GameTestHelper h) { interrupted(h, 1, true, 2, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void replacementBasinCannotInheritBucketClick(GameTestHelper h) { interrupted(h, 1, false, 9, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void spectatorChangeDuringBasinFillCancels(GameTestHelper h) { interrupted(h, 1, false, 5, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void changedBasinUpgradeMaterialCancels(GameTestHelper h) { interrupted(h, 2, false, 0, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void changedBasinUpgradeDataCancels(GameTestHelper h) { interrupted(h, 2, false, 1, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void changedBasinUpgradeCountCancels(GameTestHelper h) { interrupted(h, 2, false, 2, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void replacementBasinCannotInheritUpgradeClick(GameTestHelper h) { interrupted(h, 2, false, 9, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void uncrouchingDuringBasinUpgradeCancels(GameTestHelper h) { interrupted(h, 2, false, 8, InteractionHand.MAIN_HAND); }

    private static void deadPlayer(GameTestHelper h, int kind, boolean draining) {
        var rig = rig(h, kind, draining);
        int beforeWater = water(h, rig);
        var source = new ItemStack(kind == 2 ? Items.COPPER_BLOCK : draining ? Items.BUCKET : Items.WATER_BUCKET, kind == 2 || draining ? 3 : 1);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, source);
        var before = source.copy();
        rig.player().setHealth(0);
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(water(h, rig) == beforeWater && ItemStack.matches(source, before) && rig.player().getMainHandItem() == source
                        && (kind == 0 || ((IrrigationBasinBlockEntity) rig.entity()).upgradeTier() == 0), "Dead players cannot spend buckets or install basin upgrades");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deadPlayerCannotFillSprinkler(GameTestHelper h) { deadPlayer(h, 0, false); }
    @GameTest(template = "empty") public static void deadPlayerCannotDrainSprinkler(GameTestHelper h) { deadPlayer(h, 0, true); }
    @GameTest(template = "empty") public static void deadPlayerCannotDrainBasin(GameTestHelper h) { deadPlayer(h, 1, true); }
    @GameTest(template = "empty") public static void deadPlayerCannotUpgradeBasin(GameTestHelper h) { deadPlayer(h, 2, false); }

    private static void normalBuckets(GameTestHelper h, int kind, InteractionHand hand) {
        var rig = rig(h, kind, false);
        rig.player().setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        use(h, rig, hand);
        h.assertTrue(water(h, rig) == (kind == 0 ? 1975 : 1000) && rig.player().getItemInHand(hand).is(Items.BUCKET),
                "A normal fill transfers a full bucket and starts exactly one paid sprinkler interval");
        rig.player().setItemInHand(hand, new ItemStack(Items.BUCKET, 3));
        use(h, rig, hand);
        h.assertTrue(water(h, rig) == (kind == 0 ? 975 : 0) && rig.player().getItemInHand(hand).is(Items.BUCKET)
                        && rig.player().getItemInHand(hand).getCount() == 2 && rig.player().getInventory().countItem(Items.WATER_BUCKET) == 1,
                "Draining spends one empty bucket and returns exactly one filled bucket without discarding the stack");
        if (kind == 0) h.assertTrue(((IrrigationSprinklerBlockEntity) rig.entity()).growthActive(), "Draining the stored bucket keeps the spray already paid for");
        h.succeed();
    }
    @GameTest(template = "empty") public static void mainHandSprinklerBucketsKeepExactCosts(GameTestHelper h) { normalBuckets(h, 0, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void offhandSprinklerBucketsKeepExactCosts(GameTestHelper h) { normalBuckets(h, 0, InteractionHand.OFF_HAND); }
    @GameTest(template = "empty") public static void mainHandBasinBucketsKeepExactCosts(GameTestHelper h) { normalBuckets(h, 1, InteractionHand.MAIN_HAND); }
    @GameTest(template = "empty") public static void offhandBasinBucketsKeepExactCosts(GameTestHelper h) { normalBuckets(h, 1, InteractionHand.OFF_HAND); }

    @GameTest(template = "empty")
    public static void basinUpgradesKeepWaterAndChargeOneMaterial(GameTestHelper h) {
        var rig = rig(h, 2, false);
        for (int tier = 1; tier <= 2; tier++) {
            var hand = tier == 1 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            var source = new ItemStack(tier == 1 ? Items.COPPER_BLOCK : Items.AMETHYST_BLOCK, 3);
            rig.player().setItemInHand(hand, source);
            use(h, rig, hand);
            var basin = (IrrigationBasinBlockEntity) rig.entity();
            h.assertTrue(basin.upgradeTier() == tier && basin.wateringRadius() == 6 + 2 * tier && water(h, rig) == 1000 && source.getCount() == 2,
                    "Each basin upgrade spends one correct material, preserves water and applies its normal radius");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void basinUpgradeDuringBucketCheckKeepsNewTier(GameTestHelper h) {
        var rig = rig(h, 1, false);
        var source = new ItemStack(Items.WATER_BUCKET);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, source);
        rig.player().hook = () -> h.assertTrue(((IrrigationBasinBlockEntity) rig.entity()).upgradeTo(1), "The callback installs a newer tier");
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(water(h, rig) == 0 && ((IrrigationBasinBlockEntity) rig.entity()).upgradeTier() == 1 && rig.player().getMainHandItem() == source,
                "The old fill cannot overwrite a newer basin tier or spend its held bucket");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void sprinklerFittingRepairDuringBucketCheckStillAllowsFill(GameTestHelper h) {
        var rig = rig(h, 0, false);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        rig.player().hook = () -> h.getLevel().setBlock(rig.pos().east(), AEBlocks.IRRIGATION_PIPE.get().defaultBlockState(), 3);
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(water(h, rig) == 1975 && rig.player().getMainHandItem().is(Items.BUCKET)
                        && h.getLevel().getBlockState(rig.pos()).getValue(IrrigationSprinklerBlock.connection(Direction.EAST)),
                "A connection repair on the same head still permits filling and one paid spray");
        h.succeed();
    }
}
