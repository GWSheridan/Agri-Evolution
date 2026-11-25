package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.RareTrait;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CropSupportSafetyGameTests {
    private record Crop(BlockPos pos, BlockState state, AECropBlockEntity plant) {}

    private static Crop crop(GameTestHelper h, int tier) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var state = AEBlocks.AGRI_CROP.get().defaultBlockState().setValue(AgriCropBlock.SUPPORTED, tier > 0)
                .setValue(AgriCropBlock.SUPPORT_TIER, tier).setValue(AgriCropBlock.WEEDY, true);
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, state, 2);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        plant.setGenome(new CropGenome(71, 62, 83));
        plant.setRareTrait(RareTrait.VIGOROUS);
        return new Crop(pos, state, plant);
    }

    private static UseOnContext context(Player player, InteractionHand hand, BlockPos pos) {
        return new UseOnContext(player, hand, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = "empty")
    public static void expiredSupportTickCannotRestoreARemovedCrop(GameTestHelper h) {
        var crop = crop(h, 1);
        crop.plant().setSupportTicks(0);
        h.getLevel().setBlock(crop.pos(), Blocks.STONE.defaultBlockState(), 2);
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), crop.state(), crop.plant());
        h.assertTrue(h.getLevel().getBlockState(crop.pos()).is(Blocks.STONE), "An old expiry tick cannot restore the removed crop");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void expiredSupportTickCannotRewindCropGrowth(GameTestHelper h) {
        var crop = crop(h, 1);
        var grown = crop.state().setValue(AgriCropBlock.AGE, 7);
        h.getLevel().setBlock(crop.pos(), grown, 2);
        crop.plant().setSupportTicks(0);
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), crop.state(), crop.plant());
        h.assertTrue(h.getLevel().getBlockState(crop.pos()) == grown, "A stale expiry leaves the newer growth state alone");
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), grown, crop.plant());
        h.assertTrue(h.getLevel().getBlockState(crop.pos()).getValue(AgriCropBlock.AGE) == 7
                        && CropSupportItem.tier(h.getLevel().getBlockState(crop.pos())) == 0,
                "The current expiry removes only the sticks and retains mature growth");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleSupportTickCannotChargeNewerWear(GameTestHelper h) {
        var crop = crop(h, 1);
        var grown = crop.state().setValue(AgriCropBlock.AGE, 1);
        h.getLevel().setBlock(crop.pos(), grown, 2);
        crop.plant().setSupportTicks(50);
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), crop.state(), crop.plant());
        h.assertTrue(crop.plant().supportTicks() == 50, "A stale tick cannot consume the current support's lifetime");
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), grown, crop.plant());
        h.assertTrue(crop.plant().supportTicks() == 49, "A current tick charges exactly one tick of wear");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleObsidianTickCannotUndoAnUpgradeStateChange(GameTestHelper h) {
        var crop = crop(h, 3);
        var iron = crop.state().setValue(AgriCropBlock.SUPPORT_TIER, 2);
        h.getLevel().setBlock(crop.pos(), iron, 2);
        crop.plant().setSupportTicks(700);
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), crop.state(), crop.plant());
        h.assertTrue(h.getLevel().getBlockState(crop.pos()) == iron && crop.plant().supportTicks() == 700,
                "An old obsidian weed cleanup cannot restore its old tier over a newer support");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedCropEntityCannotExpireAReplacementPlant(GameTestHelper h) {
        var crop = crop(h, 1);
        crop.plant().setSupportTicks(0);
        h.getLevel().setBlock(crop.pos(), Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(crop.pos(), crop.state(), 2);
        var replacement = (AECropBlockEntity) h.getLevel().getBlockEntity(crop.pos());
        replacement.setSupportTicks(900);
        AECropBlockEntity.tickSupport(h.getLevel(), crop.pos(), crop.state(), crop.plant());
        h.assertTrue(h.getLevel().getBlockEntity(crop.pos()) == replacement
                        && h.getLevel().getBlockState(crop.pos()) == crop.state() && replacement.supportTicks() == 900,
                "A detached entity cannot expire a replacement with the same block state");
        h.succeed();
    }

    private static void savedWear(GameTestHelper h, int tier) {
        var crop = crop(h, tier);
        var tag = new CompoundTag();
        crop.plant().saveAdditional(tag, h.getLevel().registryAccess());
        tag.putInt("SupportTicks", Integer.MAX_VALUE);
        crop.plant().loadAdditional(tag, h.getLevel().registryAccess());
        h.assertTrue(crop.plant().supportTicks() == CropSupportItem.lifetime(tier), "Loaded wear cannot exceed that tier's full lifetime");
        h.succeed();
    }

    @GameTest(template = "empty") public static void loadedWoodSupportLifeIsBounded(GameTestHelper h) { savedWear(h, 1); }
    @GameTest(template = "empty") public static void loadedIronSupportLifeIsBounded(GameTestHelper h) { savedWear(h, 2); }

    @GameTest(template = "empty")
    public static void obsidianRecoveryIgnoresAnOldFiniteCountdown(GameTestHelper h) {
        var crop = crop(h, 3);
        var tag = new CompoundTag();
        crop.plant().saveAdditional(tag, h.getLevel().registryAccess());
        tag.putInt("SupportTicks", 0);
        crop.plant().loadAdditional(tag, h.getLevel().registryAccess());
        var recovered = crop.plant().recoverSupport();
        h.assertTrue(crop.plant().supportTicks() == -1 && recovered.is(AEItems.OBSIDIAN_CROP_SUPPORT.get()),
                "Permanent sticks stay recoverable even when an older finite countdown was saved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorsCannotInstallCropSupports(GameTestHelper h) {
        var crop = crop(h, 1);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 2));
        var result = AEItems.IRON_CROP_SUPPORT.get().useOn(context(player, InteractionHand.MAIN_HAND, crop.pos()));
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(crop.pos()) == crop.state()
                        && player.getMainHandItem().getCount() == 2,
                "Spectator installation keeps both the crop and held supports unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorsCannotRecoverCropSupports(GameTestHelper h) {
        var crop = crop(h, 2);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.SPANNER.get()));
        var result = AEItems.SPANNER.get().useOn(context(player, InteractionHand.MAIN_HAND, crop.pos()));
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(crop.pos()) == crop.state(),
                "Spectator recovery leaves installed supports in place");
        h.succeed();
    }

    private static final class CallbackPlayer extends Player {
        Runnable onPermission = () -> {};
        CallbackPlayer(ServerLevel level) { super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "support-access")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
            var callback = onPermission;
            onPermission = () -> {};
            callback.run();
            return true;
        }
    }

    @GameTest(template = "empty")
    public static void supportInstallationPreservesPermissionCallbackReplacement(GameTestHelper h) {
        var crop = crop(h, 1);
        var player = new CallbackPlayer(h.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 2));
        player.onPermission = () -> h.getLevel().setBlock(crop.pos(), Blocks.WHEAT.defaultBlockState(), 2);
        var result = AEItems.IRON_CROP_SUPPORT.get().useOn(context(player, InteractionHand.MAIN_HAND, crop.pos()));
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(crop.pos()).is(Blocks.WHEAT)
                        && player.getMainHandItem().getCount() == 2,
                "A permission callback's replacement cannot be overwritten or charged for an upgrade");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void supportRecoveryPreservesSameStateReplacement(GameTestHelper h) {
        var crop = crop(h, 2);
        var player = new CallbackPlayer(h.getLevel());
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AEItems.SPANNER.get()));
        player.onPermission = () -> {
            h.getLevel().setBlock(crop.pos(), Blocks.AIR.defaultBlockState(), 2);
            h.getLevel().setBlock(crop.pos(), crop.state(), 2);
            ((AECropBlockEntity) h.getLevel().getBlockEntity(crop.pos())).setSupportTicks(700);
        };
        var result = AEItems.SPANNER.get().useOn(context(player, InteractionHand.MAIN_HAND, crop.pos()));
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(crop.pos()) == crop.state()
                        && ((AECropBlockEntity) h.getLevel().getBlockEntity(crop.pos())).supportTicks() == 700,
                "A recovery click cannot detach sticks from a replacement with the same state");
        h.succeed();
    }

    private static void changedHand(GameTestHelper h, boolean recovery) {
        var crop = crop(h, 1);
        var player = new CallbackPlayer(h.getLevel());
        player.setShiftKeyDown(recovery);
        var item = recovery ? AEItems.SPANNER.get() : AEItems.IRON_CROP_SUPPORT.get();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item, recovery ? 1 : 2));
        player.onPermission = () -> player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT, 3));
        var result = item.useOn(context(player, InteractionHand.OFF_HAND, crop.pos()));
        h.assertTrue(result == InteractionResult.FAIL && h.getLevel().getBlockState(crop.pos()) == crop.state()
                        && player.getOffhandItem().is(Items.DIRT) && player.getOffhandItem().getCount() == 3,
                "A changed held item invalidates the old support action");
        h.succeed();
    }

    @GameTest(template = "empty") public static void changedHeldSupportsCancelInstallation(GameTestHelper h) { changedHand(h, false); }
    @GameTest(template = "empty") public static void changedHeldSpannerCancelsRecovery(GameTestHelper h) { changedHand(h, true); }

    @GameTest(template = "empty")
    public static void offhandWornSupportUpgradeAndFullInventoryRecovery(GameTestHelper h) {
        var crop = crop(h, 1);
        crop.plant().setSupportTicks(1234);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(crop.pos()));
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        var iron = new ItemStack(AEItems.IRON_CROP_SUPPORT.get(), 2);
        CustomData.update(DataComponents.CUSTOM_DATA, iron, tag -> tag.putInt("SupportTicks", 50000));
        player.setItemInHand(InteractionHand.OFF_HAND, iron);
        h.assertTrue(AEItems.IRON_CROP_SUPPORT.get().useOn(context(player, InteractionHand.OFF_HAND, crop.pos())).consumesAction(),
                "Worn iron supports can be installed from the offhand");
        h.assertTrue(CropSupportItem.tier(h.getLevel().getBlockState(crop.pos())) == 2 && crop.plant().supportTicks() == 50000
                        && crop.plant().genome().equals(new CropGenome(71, 62, 83)) && crop.plant().rareTrait() == RareTrait.VIGOROUS
                        && iron.getCount() == 1,
                "An upgrade retains crop data and both installed and returned wear");
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AEItems.SPANNER.get()));
        h.assertTrue(AEItems.SPANNER.get().useOn(context(player, InteractionHand.OFF_HAND, crop.pos())).consumesAction(),
                "Offhand Spanner recovery works with a full inventory");
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(crop.pos()).inflate(3));
        h.assertTrue(drops.stream().filter(e -> e.getItem().is(AEItems.CROP_SUPPORT.get()) && CropSupportItem.remaining(e.getItem()) == 1234)
                        .mapToInt(e -> e.getItem().getCount()).sum() == 1
                        && drops.stream().filter(e -> e.getItem().is(AEItems.IRON_CROP_SUPPORT.get()) && CropSupportItem.remaining(e.getItem()) == 50000)
                        .mapToInt(e -> e.getItem().getCount()).sum() == 1,
                "Full inventory overflow drops each worn support exactly once");
        h.succeed();
    }
}
