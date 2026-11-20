package com.agrievolution.content;

import com.agrievolution.genetics.*;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CropSupportInteractionGameTests {
    private static final class HookPlayer extends Player {
        Runnable hook;
        boolean spectator, creative, denied;
        int permissions;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "support-use"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return creative; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level level) { setLevel(level); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
            permissions++;
            if (hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            // A permission provider may have granted this check before changing the actor.
            return !denied;
        }
    }
    private record Rig(BlockPos pos, AECropBlockEntity plant, HookPlayer player, ItemStack held, boolean recovery) {}
    private static Rig rig(GameTestHelper h, boolean recovery) {
        var pos = h.absolutePos(new BlockPos(4, 210, 4));
        var state = AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 3)
                .setValue(AgriCropBlock.SUPPORTED, true).setValue(AgriCropBlock.SUPPORT_TIER, recovery ? 2 : 1)
                .setValue(AgriCropBlock.WEEDY, true);
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, state, 2);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        plant.setSupportTicks(1234);
        plant.setGenome(new CropGenome(71, 82, 63));
        plant.setRareTrait(RareTrait.VIGOROUS);
        plant.setLineage(new HybridLineageStorage.Lineage(CropSpecies.CRIMSON_CLOVER, CropSpecies.AZURE_FLAX));
        plant.setVariety("Support trial");
        var player = new HookPlayer(h.getLevel(), pos);
        player.setShiftKeyDown(recovery);
        var held = new ItemStack(recovery ? AEItems.SPANNER.get() : AEItems.IRON_CROP_SUPPORT.get(), recovery ? 1 : 2);
        held.set(DataComponents.CUSTOM_NAME, Component.literal("Field equipment"));
        if (!recovery) CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.putInt("SupportTicks", 50000));
        player.setItemInHand(InteractionHand.OFF_HAND, held);
        return new Rig(pos, plant, player, held, recovery);
    }
    private static UseOnContext context(Rig r, BlockPos pos, ItemStack stack) {
        return new UseOnContext(r.player().level(), r.player(), InteractionHand.OFF_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)) {};
    }
    private static InteractionResult use(Rig r) { return r.held().getItem().useOn(context(r, r.pos(), r.held())); }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity crop) {
        var tag = new CompoundTag();
        crop.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void changed(GameTestHelper h, boolean recovery, int mutation) {
        var r = rig(h, recovery);
        var beforeState = h.getLevel().getBlockState(r.pos());
        var expected = new CompoundTag[1];
        r.player().hook = () -> {
            switch (mutation) {
                case 0 -> r.player().setHealth(0);
                case 1 -> r.player().spectator = true;
                case 2 -> r.player().getAbilities().mayBuild = false;
                case 3 -> r.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 4 -> r.player().setShiftKeyDown(false);
                case 5 -> r.plant().setGenome(new CropGenome(12, 34, 56));
                case 6 -> r.plant().setRareTrait(RareTrait.TENACIOUS);
                case 7 -> r.plant().setLineage(new HybridLineageStorage.Lineage(CropSpecies.CORN, CropSpecies.PEAS));
                case 8 -> r.plant().setVariety("New support trial");
                case 9 -> r.plant().addNutrition(11);
                case 10 -> r.plant().setSupportTicks(333);
                case 11 -> r.plant().setTended();
                case 12 -> r.plant().setClipped(true);
                case 13 -> r.plant().setCrossed(true);
                case 14 -> r.plant().markPollinated(h.getLevel().getGameTime() + 500);
                default -> throw new AssertionError("Unknown support interruption");
            }
            expected[0] = saved(h, r.plant());
        };
        var result = use(r);
        h.assertTrue(r.player().hook == null, "The permission callback ran");
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(r.pos()) == beforeState,
                "Changed access or crop data cancels the old support action");
        h.assertTrue(expected[0].equals(saved(h, r.plant())), "The newer crop data and support wear remain intact");
        h.assertTrue(r.held().getCount() == (recovery ? 1 : 2) && r.held().getDamageValue() == 0,
                "Interrupted support work does not spend the held item");
        h.assertTrue(r.player().getInventory().countItem(AEItems.CROP_SUPPORT.get()) == 0
                        && r.player().getInventory().countItem(AEItems.IRON_CROP_SUPPORT.get()) == (recovery ? 0 : 2),
                "Interrupted work cannot return installed sticks");
        h.succeed();
    }
    @GameTest(template = "empty") public static void deathDuringSupportInstallationCancelsUpgrade(GameTestHelper h) { changed(h, false, 0); }
    @GameTest(template = "empty") public static void spectatorDuringSupportInstallationCancelsUpgrade(GameTestHelper h) { changed(h, false, 1); }
    @GameTest(template = "empty") public static void revokedBuildAccessCancelsSupportInstallation(GameTestHelper h) { changed(h, false, 2); }
    @GameTest(template = "empty") public static void worldChangeDuringSupportInstallationCancelsUpgrade(GameTestHelper h) { changed(h, false, 3); }
    @GameTest(template = "empty") public static void deathDuringSupportRecoveryKeepsSticksInstalled(GameTestHelper h) { changed(h, true, 0); }
    @GameTest(template = "empty") public static void spectatorDuringSupportRecoveryKeepsSticksInstalled(GameTestHelper h) { changed(h, true, 1); }
    @GameTest(template = "empty") public static void revokedBuildAccessCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 2); }
    @GameTest(template = "empty") public static void worldChangeDuringSupportRecoveryKeepsSticksInstalled(GameTestHelper h) { changed(h, true, 3); }
    @GameTest(template = "empty") public static void leavingCrouchCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 4); }
    @GameTest(template = "empty") public static void changedGenesCancelSupportUpgrade(GameTestHelper h) { changed(h, false, 5); }
    @GameTest(template = "empty") public static void changedGenesCancelSupportRecovery(GameTestHelper h) { changed(h, true, 5); }
    @GameTest(template = "empty") public static void changedRareTraitCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 6); }
    @GameTest(template = "empty") public static void changedRareTraitCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 6); }
    @GameTest(template = "empty") public static void changedAncestryCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 7); }
    @GameTest(template = "empty") public static void changedAncestryCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 7); }
    @GameTest(template = "empty") public static void changedVarietyCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 8); }
    @GameTest(template = "empty") public static void changedVarietyCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 8); }
    @GameTest(template = "empty") public static void changedNutritionCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 9); }
    @GameTest(template = "empty") public static void changedNutritionCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 9); }
    @GameTest(template = "empty") public static void newerSupportWearCancelsUpgrade(GameTestHelper h) { changed(h, false, 10); }
    @GameTest(template = "empty") public static void newerSupportWearCancelsRecovery(GameTestHelper h) { changed(h, true, 10); }
    @GameTest(template = "empty") public static void nestedTendingCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 11); }
    @GameTest(template = "empty") public static void nestedClippingCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 12); }
    @GameTest(template = "empty") public static void spentCrossingCreditCancelsSupportUpgrade(GameTestHelper h) { changed(h, false, 13); }
    @GameTest(template = "empty") public static void newerPollinationCancelsSupportRecovery(GameTestHelper h) { changed(h, true, 14); }

    @GameTest(template = "empty") public static void deadPlayersCannotInstallSupports(GameTestHelper h) {
        var r = rig(h, false);
        r.player().setHealth(0);
        var state = h.getLevel().getBlockState(r.pos());
        use(r);
        h.assertTrue(h.getLevel().getBlockState(r.pos()) == state && r.held().getCount() == 2 && r.player().permissions == 0,
                "Dead players are rejected before permission callbacks and cannot upgrade a plant");
        h.succeed();
    }
    @GameTest(template = "empty") public static void detachedSupportContextIsRejectedBeforePermissions(GameTestHelper h) {
        var r = rig(h, false);
        var state = h.getLevel().getBlockState(r.pos());
        r.held().getItem().useOn(context(r, r.pos(), r.held().copy()));
        h.assertTrue(r.player().permissions == 0 && h.getLevel().getBlockState(r.pos()) == state && r.held().getCount() == 2,
                "A detached stack cannot invoke support permissions or edit the field");
        h.succeed();
    }
    @GameTest(template = "empty") public static void mismatchedCropWorldRejectsSupportActions(GameTestHelper h) {
        for (boolean recovery : new boolean[]{false, true}) {
            var r = rig(h, recovery);
            var state = h.getLevel().getBlockState(r.pos());
            r.plant().setLevel(h.getLevel().getServer().getLevel(Level.NETHER));
            use(r);
            h.assertTrue(h.getLevel().getBlockState(r.pos()) == state && r.player().permissions == 0,
                    "A crop attached to another world cannot install or return sticks");
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void unloadedSupportClicksDoNotLoadFarawayChunks(GameTestHelper h) {
        var r = rig(h, false);
        var remote = new BlockPos(r.pos().getX() + 24000, r.pos().getY(), r.pos().getZ() + 24000);
        h.assertTrue(!h.getLevel().hasChunkAt(remote), "The test target begins unloaded");
        r.held().getItem().useOn(context(r, remote, r.held()));
        h.assertTrue(!h.getLevel().hasChunkAt(remote) && r.held().getCount() == 2, "An old support click does not load a remote chunk");
        h.succeed();
    }

    private static void normal(GameTestHelper h, boolean recovery, boolean creative, boolean denied) {
        var r = rig(h, recovery);
        r.player().creative = creative;
        r.player().getAbilities().instabuild = creative;
        r.player().denied = denied;
        var state = h.getLevel().getBlockState(r.pos());
        var before = saved(h, r.plant());
        var result = use(r);
        if (denied) {
            h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(r.pos()) == state
                            && before.equals(saved(h, r.plant())) && r.held().getCount() == (recovery ? 1 : 2), "Denied work has no cost or crop edits");
        } else {
            h.assertTrue(result.consumesAction() && CropSupportItem.tier(h.getLevel().getBlockState(r.pos())) == (recovery ? 0 : 2), "Normal installation and recovery still work");
            h.assertTrue(r.held().getCount() == (recovery ? 1 : creative ? 2 : 1), "Creative retains supports; Survival spends one support");
            before.putInt("SupportTicks", recovery ? -1 : 50000);
            h.assertTrue(before.equals(saved(h, r.plant())), "Support work preserves all other crop data");
            int expected = creative ? 0 : 1;
            var returned = r.player().getInventory().items.stream().filter(s -> s.is(recovery ? AEItems.IRON_CROP_SUPPORT.get() : AEItems.CROP_SUPPORT.get())).toList();
            h.assertTrue(returned.stream().mapToInt(ItemStack::getCount).sum() == expected
                            && returned.stream().allMatch(s -> CropSupportItem.remaining(s) == 1234), "Only Survival returns the old support with its exact remaining life");
        }
        h.succeed();
    }
    @GameTest(template = "empty") public static void normalWornSupportUpgradeRetainsCropAndReturnsWear(GameTestHelper h) { normal(h, false, false, false); }
    @GameTest(template = "empty") public static void normalSpannerRecoveryRetainsCropAndReturnsWear(GameTestHelper h) { normal(h, true, false, false); }
    @GameTest(template = "empty") public static void creativeSupportUpgradeDoesNotDuplicateOldSticks(GameTestHelper h) { normal(h, false, true, false); }
    @GameTest(template = "empty") public static void creativeSupportRecoveryDoesNotReturnSticks(GameTestHelper h) { normal(h, true, true, false); }
    @GameTest(template = "empty") public static void deniedSupportInstallationHasNoCost(GameTestHelper h) { normal(h, false, false, true); }
    @GameTest(template = "empty") public static void deniedSupportRecoveryHasNoCost(GameTestHelper h) { normal(h, true, false, true); }
}
