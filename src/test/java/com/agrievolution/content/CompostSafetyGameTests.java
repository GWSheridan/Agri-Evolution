package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CompostSafetyGameTests {
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "compost-use"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(int action, BlockPos pos, HookPlayer player, AECropBlockEntity plant) {}
    private static Rig rig(GameTestHelper h, int action) {
        var pos = h.absolutePos(new BlockPos(4, 190 + action * 10, 4));
        AECropBlockEntity plant = null;
        if (action == 2) {
            h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
            h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 2), 2);
            plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
            plant.setGenome(new CropGenome(63, 52, 41));
        } else {
            h.getLevel().setBlock(pos, AEBlocks.COMPOST_BIN.get().defaultBlockState()
                    .setValue(CompostBinBlock.FILL, action == 0 ? 2 : CompostBinBlock.READY), 3);
        }
        return new Rig(action, pos, new HookPlayer(h.getLevel(), pos), plant);
    }
    private static ItemStack source(int action) {
        return new ItemStack(action == 0 ? Items.OAK_LEAVES : action == 1 ? Items.STICK : AEItems.NUTRIENT_COMPOST.get(), 3);
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void use(GameTestHelper h, Rig rig, InteractionHand hand) {
        var state = h.getLevel().getBlockState(rig.pos());
        if (rig.action() == 0) AEBlocks.COMPOST_BIN.get().useItemOn(rig.player().getItemInHand(hand), state,
                h.getLevel(), rig.pos(), rig.player(), hand, hit(rig.pos()));
        else if (rig.action() == 1) AEBlocks.COMPOST_BIN.get().useWithoutItem(state, h.getLevel(), rig.pos(), rig.player(), hit(rig.pos()));
        else AEItems.NUTRIENT_COMPOST.get().useOn(new UseOnContext(rig.player(), hand, hit(rig.pos())));
    }
    private static void interrupted(GameTestHelper h, int action, int mutation) {
        var rig = rig(h, action);
        var hand = action == 1 || mutation % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held = source(action);
        rig.player().setItemInHand(hand, held);
        var expected = new ItemStack[]{held.copy()};
        var state = h.getLevel().getBlockState(rig.pos());
        var before = action == 2 ? saved(h, rig.plant()) : null;
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.IRON_INGOT, 4));
                case 1 -> held.set(DataComponents.CUSTOM_NAME, Component.literal("Changed during access check"));
                case 2 -> held.grow(1);
                case 3 -> rig.player().setItemInHand(hand, held.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().getAbilities().mayBuild = false;
                case 7 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                default -> throw new AssertionError("Unknown compost interruption");
            }
            expected[0] = rig.player().getItemInHand(hand).copy();
        };
        use(h, rig, hand);
        h.assertTrue(rig.player().hook == null, "The permission callback ran");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getItemInHand(hand)), "An interrupted action retains the newer held item and its data");
        h.assertTrue(h.getLevel().getBlockState(rig.pos()) == state, "An interrupted action changes neither bin fill nor crop growth");
        if (action == 2) h.assertTrue(before.equals(saved(h, rig.plant())), "Interrupted feeding retains the crop's nutrition, genes and other saved data");
        else h.assertTrue(rig.player().getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0, "An interrupted collection creates no compost reward");
        h.succeed();
    }

    @GameTest(template = "empty") public static void swappedMaterialCancelsCompostDeposit(GameTestHelper h) { interrupted(h, 0, 0); }
    @GameTest(template = "empty") public static void renamedMaterialCancelsCompostDeposit(GameTestHelper h) { interrupted(h, 0, 1); }
    @GameTest(template = "empty") public static void changedMaterialCountCancelsCompostDeposit(GameTestHelper h) { interrupted(h, 0, 2); }
    @GameTest(template = "empty") public static void matchingMaterialReplacementCancelsCompostDeposit(GameTestHelper h) { interrupted(h, 0, 3); }
    @GameTest(template = "empty") public static void deathDuringCompostDepositCancelsAction(GameTestHelper h) { interrupted(h, 0, 4); }
    @GameTest(template = "empty") public static void spectatorDuringCompostDepositCancelsAction(GameTestHelper h) { interrupted(h, 0, 5); }
    @GameTest(template = "empty") public static void revokedBuildDuringCompostDepositCancelsAction(GameTestHelper h) { interrupted(h, 0, 6); }
    @GameTest(template = "empty") public static void dimensionChangeDuringCompostDepositCancelsAction(GameTestHelper h) { interrupted(h, 0, 7); }
    @GameTest(template = "empty") public static void swappedHandCancelsCompostCollection(GameTestHelper h) { interrupted(h, 1, 0); }
    @GameTest(template = "empty") public static void renamedHandCancelsCompostCollection(GameTestHelper h) { interrupted(h, 1, 1); }
    @GameTest(template = "empty") public static void changedHandCountCancelsCompostCollection(GameTestHelper h) { interrupted(h, 1, 2); }
    @GameTest(template = "empty") public static void matchingHandReplacementCancelsCompostCollection(GameTestHelper h) { interrupted(h, 1, 3); }
    @GameTest(template = "empty") public static void deathDuringCompostCollectionCancelsAction(GameTestHelper h) { interrupted(h, 1, 4); }
    @GameTest(template = "empty") public static void spectatorDuringCompostCollectionCancelsAction(GameTestHelper h) { interrupted(h, 1, 5); }
    @GameTest(template = "empty") public static void revokedBuildDuringCompostCollectionCancelsAction(GameTestHelper h) { interrupted(h, 1, 6); }
    @GameTest(template = "empty") public static void dimensionChangeDuringCompostCollectionCancelsAction(GameTestHelper h) { interrupted(h, 1, 7); }
    @GameTest(template = "empty") public static void swappedCompostCancelsCropFeeding(GameTestHelper h) { interrupted(h, 2, 0); }
    @GameTest(template = "empty") public static void renamedCompostCancelsCropFeeding(GameTestHelper h) { interrupted(h, 2, 1); }
    @GameTest(template = "empty") public static void changedCompostCountCancelsCropFeeding(GameTestHelper h) { interrupted(h, 2, 2); }
    @GameTest(template = "empty") public static void matchingCompostReplacementCancelsCropFeeding(GameTestHelper h) { interrupted(h, 2, 3); }
    @GameTest(template = "empty") public static void deathDuringCropFeedingCancelsAction(GameTestHelper h) { interrupted(h, 2, 4); }
    @GameTest(template = "empty") public static void spectatorDuringCropFeedingCancelsAction(GameTestHelper h) { interrupted(h, 2, 5); }
    @GameTest(template = "empty") public static void revokedBuildDuringCropFeedingCancelsAction(GameTestHelper h) { interrupted(h, 2, 6); }
    @GameTest(template = "empty") public static void dimensionChangeDuringCropFeedingCancelsAction(GameTestHelper h) { interrupted(h, 2, 7); }

    @GameTest(template = "empty")
    public static void deadPlayersCannotUseCompostWorkflow(GameTestHelper h) {
        for (int action = 0; action < 3; action++) {
            var rig = rig(h, action);
            var held = source(action);
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
            var state = h.getLevel().getBlockState(rig.pos());
            rig.player().setHealth(0);
            use(h, rig, InteractionHand.MAIN_HAND);
            h.assertTrue(h.getLevel().getBlockState(rig.pos()) == state && held.getCount() == 3,
                    "Dead players cannot deposit, collect or feed crops");
            if (action < 2) h.assertTrue(rig.player().getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0, "A dead collector receives no reward");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void replacementCropCannotInheritPendingCompost(GameTestHelper h) {
        var rig = rig(h, 2);
        var held = source(2);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        var before = saved(h, rig.plant());
        var expected = new CompoundTag[1];
        rig.player().hook = () -> {
            h.getLevel().removeBlockEntity(rig.pos());
            var replacement = new AECropBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
            replacement.loadAdditional(before, h.getLevel().registryAccess());
            replacement.setGenome(new CropGenome(81, 72, 63));
            h.getLevel().setBlockEntity(replacement);
            expected[0] = saved(h, replacement);
        };
        use(h, rig, InteractionHand.OFF_HAND);
        var current = (AECropBlockEntity) h.getLevel().getBlockEntity(rig.pos());
        h.assertTrue(expected[0] != null && expected[0].equals(saved(h, current)) && before.equals(saved(h, rig.plant()))
                        && current.getBlockState().getValue(AgriCropBlock.AGE) == 2 && held.getCount() == 3,
                "Replacing the planted entity cancels feeding even when the crop block state is identical");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unrelatedHeldItemCannotBeSpentAsNutrientCompost(GameTestHelper h) {
        var rig = rig(h, 2);
        var held = new ItemStack(Items.STICK, 3);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var before = saved(h, rig.plant());
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(held.getCount() == 3 && before.equals(saved(h, rig.plant()))
                        && h.getLevel().getBlockState(rig.pos()).getValue(AgriCropBlock.AGE) == 2,
                "An invalid compost dispatch cannot feed the crop by consuming an unrelated item");
        h.succeed();
    }

    private static void normalCosts(GameTestHelper h, boolean creative) {
        for (int action = 0; action < 3; action++) {
            var rig = rig(h, action);
            rig.player().getAbilities().instabuild = creative;
            for (var hand : InteractionHand.values()) {
                var held = source(action);
                rig.player().setItemInHand(action == 1 ? InteractionHand.MAIN_HAND : hand, held);
                if (action == 1) h.getLevel().setBlock(rig.pos(), AEBlocks.COMPOST_BIN.get().defaultBlockState().setValue(CompostBinBlock.FILL, CompostBinBlock.READY), 3);
                if (action == 2) h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.AGE, 2), 2);
                int before = action == 0 ? h.getLevel().getBlockState(rig.pos()).getValue(CompostBinBlock.FILL)
                        : action == 1 ? rig.player().getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) : rig.plant().soilNutrition();
                use(h, rig, action == 1 ? InteractionHand.MAIN_HAND : hand);
                h.assertTrue(held.getCount() == (creative || action == 1 ? 3 : 2), "Only a successful Survival deposit or crop feed spends one item");
                if (action == 0) h.assertTrue(h.getLevel().getBlockState(rig.pos()).getValue(CompostBinBlock.FILL) == before + 1, "One leaf adds one fill point");
                if (action == 1) h.assertTrue(rig.player().getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == before + 2
                                && h.getLevel().getBlockState(rig.pos()).getValue(CompostBinBlock.FILL) == 0, "A ready batch pays two compost and resets once");
                if (action == 2) h.assertTrue(rig.plant().soilNutrition() == before + 25 && rig.plant().genome().equals(new CropGenome(63, 52, 41))
                                && h.getLevel().getBlockState(rig.pos()).getValue(AgriCropBlock.AGE) == 4, "One compost adds two growth stages and 25 nutrition without changing genes");
            }
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void survivalCompostWorkflowKeepsExactCosts(GameTestHelper h) { normalCosts(h, false); }
    @GameTest(template = "empty") public static void creativeCompostWorkflowKeepsSupplies(GameTestHelper h) { normalCosts(h, true); }

    @GameTest(template = "empty")
    public static void matureCropKeepsCompostAndNutrition(GameTestHelper h) {
        var rig = rig(h, 2);
        h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.AGE, 7), 2);
        var held = source(2);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        var before = saved(h, rig.plant());
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(held.getCount() == 3 && before.equals(saved(h, rig.plant())), "A mature crop receives no paid growth or nutrition");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedBinFillDuringPermissionsPreservesNewBatch(GameTestHelper h) {
        for (int action = 0; action < 2; action++) {
            var rig = rig(h, action);
            var held = source(action);
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
            rig.player().hook = () -> h.getLevel().setBlock(rig.pos(), AEBlocks.COMPOST_BIN.get().defaultBlockState().setValue(CompostBinBlock.FILL, 4), 3);
            use(h, rig, InteractionHand.MAIN_HAND);
            h.assertTrue(held.getCount() == 3 && h.getLevel().getBlockState(rig.pos()).getValue(CompostBinBlock.FILL) == 4
                            && rig.player().getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0,
                    "A newer fill state is neither overwritten by a deposit nor cashed out as the old ready batch");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void newerCropGrowthDuringPermissionsCancelsCompost(GameTestHelper h) {
        var rig = rig(h, 2);
        var held = source(2);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var before = saved(h, rig.plant());
        rig.player().hook = () -> h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(AgriCropBlock.AGE, 5), 2);
        use(h, rig, InteractionHand.MAIN_HAND);
        h.assertTrue(held.getCount() == 3 && before.equals(saved(h, rig.plant()))
                        && h.getLevel().getBlockState(rig.pos()).getValue(AgriCropBlock.AGE) == 5,
                "The old feeding attempt preserves newer growth and does not spend compost or add nutrition");
        h.succeed();
    }
}
