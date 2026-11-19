package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CompostAndToolGameTests {
    private static BlockPos pos(GameTestHelper h) { return h.absolutePos(new BlockPos(4, 220, 4)); }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static BlockState bin(GameTestHelper h, BlockPos pos, int fill) {
        var state = AEBlocks.COMPOST_BIN.get().defaultBlockState().setValue(CompostBinBlock.FILL, fill);
        h.getLevel().setBlock(pos, state, 3);
        return state;
    }
    private static void deposit(GameTestHelper h, BlockPos pos, BlockState state, Player player, ItemStack material) {
        AEBlocks.COMPOST_BIN.get().useItemOn(material, state, h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit(pos));
    }
    private static void collect(GameTestHelper h, BlockPos pos, BlockState state, Player player) {
        AEBlocks.COMPOST_BIN.get().useWithoutItem(state, h.getLevel(), pos, player, hit(pos));
    }
    private static void plant(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState()
                .setValue(AgriCropBlock.AGE, 3).setValue(AgriCropBlock.WEEDY, true), 2);
    }
    private static final class DeniedPlotPlayer extends Player {
        private final BlockPos denied;
        DeniedPlotPlayer(ServerLevel level, BlockPos denied) {
            super(level, denied, 0, new GameProfile(UUID.randomUUID(), "plot-permission"));
            this.denied = denied;
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
        @Override public boolean mayUseItemAt(BlockPos target, Direction side, ItemStack stack) {
            return !target.equals(denied) && super.mayUseItemAt(target, side, stack);
        }
    }

    @GameTest(template = "empty")
    public static void readyCompostCannotBeCollectedTwiceFromOneSnapshot(GameTestHelper h) {
        var pos = pos(h);
        var ready = bin(h, pos, CompostBinBlock.READY);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        collect(h, pos, ready, player);
        var leaves = new ItemStack(Items.OAK_LEAVES, 2);
        deposit(h, pos, h.getLevel().getBlockState(pos), player, leaves);
        collect(h, pos, ready, player);
        h.assertTrue(player.getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 2
                        && h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 1 && leaves.getCount() == 1,
                "A captured ready state cannot cash out the next batch's first material");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldReadyCompostCannotReplaceAnotherBlock(GameTestHelper h) {
        var pos = pos(h);
        var ready = bin(h, pos, CompostBinBlock.READY);
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        collect(h, pos, ready, player);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE)
                        && player.getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0,
                "Late collection cannot restore a removed bin or create compost");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldDepositCannotOverwriteNewerFill(GameTestHelper h) {
        var pos = pos(h);
        var stale = bin(h, pos, 2);
        bin(h, pos, 6);
        var leaves = new ItemStack(Items.OAK_LEAVES, 3);
        deposit(h, pos, stale, h.makeMockPlayer(GameType.SURVIVAL), leaves);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 6 && leaves.getCount() == 3,
                "A stale deposit preserves the newer fill level and material");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldDepositCannotReplaceAnotherBlock(GameTestHelper h) {
        var pos = pos(h);
        var stale = bin(h, pos, 2);
        h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        var leaves = new ItemStack(Items.OAK_LEAVES, 3);
        deposit(h, pos, stale, h.makeMockPlayer(GameType.SURVIVAL), leaves);
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE) && leaves.getCount() == 3,
                "A late deposit cannot overwrite a replacement block or consume leaves");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldFermentationTickCannotMatureANewerBatch(GameTestHelper h) {
        var pos = pos(h);
        var full = bin(h, pos, CompostBinBlock.FULL);
        bin(h, pos, 2);
        AEBlocks.COMPOST_BIN.get().tick(full, h.getLevel(), pos, h.getLevel().random);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 2,
                "Only the current full state may finish fermentation");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void compostRespectsItemUsePermission(GameTestHelper h) {
        var pos = pos(h);
        var player = new DeniedPlotPlayer(h.getLevel(), pos);
        var material = new ItemStack(Items.OAK_LEAVES, 3);
        deposit(h, pos, bin(h, pos, 2), player, material);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 2 && material.getCount() == 3,
                "An item-use denial prevents depositing material");
        collect(h, pos, bin(h, pos, CompostBinBlock.READY), player);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == CompostBinBlock.READY
                        && player.getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0,
                "An item-use denial also preserves the ready batch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorsCannotDepositOrCollectCompost(GameTestHelper h) {
        var pos = pos(h);
        var player = h.makeMockPlayer(GameType.SPECTATOR);
        player.getAbilities().mayBuild = true;
        var material = new ItemStack(Items.OAK_LEAVES, 3);
        deposit(h, pos, bin(h, pos, 2), player, material);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 2 && material.getCount() == 3,
                "Spectators cannot deposit even if their build ability is set");
        collect(h, pos, bin(h, pos, CompostBinBlock.READY), player);
        h.assertTrue(player.getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 0
                        && h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == CompostBinBlock.READY,
                "Spectators cannot collect a ready batch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyMainHandYieldsToOffhandCompostMaterial(GameTestHelper h) {
        var pos = pos(h);
        var state = bin(h, pos, 0);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(AEItems.PULLED_WEEDS.get(), 3));
        var block = AEBlocks.COMPOST_BIN.get();
        var result = block.useItemOn(ItemStack.EMPTY, state, h.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit(pos));
        h.assertTrue(result == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                "The empty main hand must yield before the bin consumes the click for its status");
        block.useItemOn(player.getOffhandItem(), state, h.getLevel(), pos, player, InteractionHand.OFF_HAND, hit(pos));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 2 && player.getOffhandItem().getCount() == 2,
                "The offhand deposits one cutting for two fill points");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rewardCallbacksCannotCollectASecondCompostBatch(GameTestHelper h) {
        var pos = pos(h);
        var ready = bin(h, pos, CompostBinBlock.READY);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_LEAVES));
        boolean[] observed = {false};
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (observed[0] || !(event.getEntity() instanceof ItemEntity item) || !item.getItem().is(AEItems.NUTRIENT_COMPOST.get())) return;
            observed[0] = true;
            deposit(h, pos, h.getLevel().getBlockState(pos), player, player.getOffhandItem());
            collect(h, pos, ready, player);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { collect(h, pos, ready, player); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(e -> e.getItem().is(AEItems.NUTRIENT_COMPOST.get())).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(observed[0] && dropped == 2 && h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 1,
                "Reward callbacks can start a new batch without letting an old collection cash it out");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesOneCompostBatch(GameTestHelper h) {
        var pos = pos(h);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        player.setPos(Vec3.atCenterOf(pos));
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        collect(h, pos, bin(h, pos, CompostBinBlock.READY), player);
        collect(h, pos, h.getLevel().getBlockState(pos), player);
        int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(e -> e.getItem().is(AEItems.NUTRIENT_COMPOST.get())).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(dropped == 2 && h.getLevel().getBlockState(pos).getValue(CompostBinBlock.FILL) == 0,
                "Full Creative inventories still receive exactly one batch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullBinsKeepSpareMaterialsAndFinishNormally(GameTestHelper h) {
        var pos = pos(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var weeds = new ItemStack(AEItems.PULLED_WEEDS.get(), 3);
        deposit(h, pos, bin(h, pos, 7), player, weeds);
        var full = h.getLevel().getBlockState(pos);
        deposit(h, pos, full, player, weeds);
        h.assertTrue(full.getValue(CompostBinBlock.FILL) == CompostBinBlock.FULL && weeds.getCount() == 2,
                "The last cutting fills the bin; additional deposits cost nothing");
        AEBlocks.COMPOST_BIN.get().tick(full, h.getLevel(), pos, h.getLevel().random);
        collect(h, pos, h.getLevel().getBlockState(pos), player);
        h.assertTrue(player.getInventory().countItem(AEItems.NUTRIENT_COMPOST.get()) == 2,
                "A valid full batch still matures and pays normally");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rakeCannotChangeSoilWithoutItemPermission(GameTestHelper h) {
        var pos = pos(h);
        plant(h, pos);
        var player = new DeniedPlotPlayer(h.getLevel(), pos.below());
        var rake = new ItemStack(AEItems.RAKE_IRON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rake);
        rake.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.WEEDY)
                        && h.getLevel().getBlockState(pos.below()).getValue(FarmBlock.MOISTURE) == 0
                        && rake.getDamageValue() == 0 && player.getInventory().countItem(AEItems.PULLED_WEEDS.get()) == 0,
                "Denied soil use keeps weeds, moisture, tool durability and rewards unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void areaRakeSkipsDeniedSoilAndClearsAllowedNeighbor(GameTestHelper h) {
        var pos = pos(h);
        plant(h, pos);
        plant(h, pos.east());
        var player = new DeniedPlotPlayer(h.getLevel(), pos.below());
        player.setShiftKeyDown(true);
        var rake = new ItemStack(AEItems.RAKE_IRON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, rake);
        rake.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(AgriCropBlock.WEEDY)
                        && !h.getLevel().getBlockState(pos.east()).getValue(AgriCropBlock.WEEDY)
                        && rake.getDamageValue() == 1 && player.getInventory().countItem(AEItems.PULLED_WEEDS.get()) == 1,
                "Area weeding skips denied ground and charges only for its allowed neighbor");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trowelCannotOverwriteAReplacementFromTheToolEvent(GameTestHelper h) {
        var pos = pos(h);
        h.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var trowel = new ItemStack(AEItems.TROWEL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, trowel);
        boolean[] observed = {false};
        Consumer<BlockEvent.BlockToolModificationEvent> listener = event -> {
            if (!event.getPos().equals(pos) || event.isSimulated()) return;
            observed[0] = true;
            h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            event.setFinalState(Blocks.FARMLAND.defaultBlockState());
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { trowel.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos))); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(observed[0] && h.getLevel().getBlockState(pos).is(Blocks.STONE)
                        && trowel.getDamageValue() == 0 && !player.getCooldowns().isOnCooldown(AEItems.TROWEL.get()),
                "A replaced tilling target remains intact and costs no durability or cooldown");
        h.succeed();
    }
}
