package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class GardenToolSafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger;
        int calls, triggerCall = 1;
        Runnable hook;
        boolean spectator;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "garden-use"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return spectator; }
        void changeWorld(Level world) { setLevel(world); }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (pos.equals(trigger) && ++calls == triggerCall && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(int action, BlockPos clicked, BlockPos plantPos, AECropBlockEntity plant, HookPlayer player) {}
    private static AECropBlockEntity plant(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        h.getLevel().setBlock(pos, AEBlocks.TOMATO_CROP.get().defaultBlockState().setValue(AgriCropBlock.AGE, 3).setValue(AgriCropBlock.WEEDY, true), 2);
        var plant = (AECropBlockEntity) h.getLevel().getBlockEntity(pos);
        plant.setGenome(new CropGenome(81, 72, 63));
        return plant;
    }
    private static Rig rig(GameTestHelper h, int action) {
        var pos = h.absolutePos(new BlockPos(4, 190 + action * 8, 4));
        AECropBlockEntity plant = null;
        if (action == 2) {
            h.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
            h.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        } else {
            plant = plant(h, pos);
            if (action == 3) plant(h, pos.east());
        }
        var player = new HookPlayer(h.getLevel(), pos);
        player.setShiftKeyDown(action >= 2);
        return new Rig(action, pos, action == 2 ? pos.above() : pos, plant, player);
    }
    private static GardenToolItem item(int action) {
        return action == 1 ? AEItems.RAKE_WOOD.get() : action == 3 ? AEItems.RAKE_IRON.get() : AEItems.TROWEL.get();
    }
    private static ItemStack tool(int action) {
        var stack = new ItemStack(item(action));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Field tool"));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("GardenUse", 7));
        return stack;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static void use(Rig rig, InteractionHand hand) { item(rig.action()).useOn(new UseOnContext(rig.player(), hand, hit(rig.clicked()))); }
    private static CompoundTag saved(GameTestHelper h, AECropBlockEntity plant) {
        var tag = new CompoundTag();
        plant.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void interrupted(GameTestHelper h, int action, int mutation, boolean late) {
        var rig = rig(h, action);
        var hand = action == 3 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held = tool(action);
        rig.player().setItemInHand(hand, held);
        rig.player().trigger = late && action != 2 ? rig.plantPos().below() : rig.clicked();
        rig.player().triggerCall = late && action == 2 ? 2 : 1;
        var expectedHeld = new ItemStack[]{held.copy()};
        var expectedSoil = new BlockState[1];
        var expectedCrop = new BlockState[1];
        var expectedPlant = new CompoundTag[1];
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.IRON_INGOT, 4));
                case 1 -> held.set(DataComponents.CUSTOM_NAME, Component.literal("Changed field tool"));
                case 2 -> CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.putInt("GardenUse", 8));
                case 3 -> rig.player().setItemInHand(hand, held.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> rig.player().spectator = true;
                case 6 -> rig.player().getAbilities().mayBuild = false;
                case 7 -> rig.player().changeWorld(h.getLevel().getServer().getLevel(Level.NETHER));
                case 8 -> rig.player().setShiftKeyDown(!rig.player().isShiftKeyDown());
                case 9 -> {
                    var tag = saved(h, rig.plant());
                    h.getLevel().removeBlockEntity(rig.plantPos());
                    var replacement = new AECropBlockEntity(rig.plantPos(), h.getLevel().getBlockState(rig.plantPos()));
                    replacement.loadAdditional(tag, h.getLevel().registryAccess());
                    replacement.setGenome(new CropGenome(41, 52, 63));
                    h.getLevel().setBlockEntity(replacement);
                }
                case 10 -> h.getLevel().setBlock(rig.plantPos(), h.getLevel().getBlockState(rig.plantPos()).setValue(AgriCropBlock.AGE, 6), 2);
                case 11 -> h.getLevel().setBlock(rig.plantPos().below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 1), 2);
                case 12 -> h.getLevel().setBlock(rig.plantPos(), Blocks.STONE.defaultBlockState(), 2);
                default -> throw new AssertionError("Unknown garden interruption");
            }
            expectedHeld[0] = rig.player().getItemInHand(hand).copy();
            expectedSoil[0] = h.getLevel().getBlockState(rig.plantPos().below());
            expectedCrop[0] = h.getLevel().getBlockState(rig.plantPos());
            if (h.getLevel().getBlockEntity(rig.plantPos()) instanceof AECropBlockEntity current) expectedPlant[0] = saved(h, current);
        };
        use(rig, hand);
        h.assertTrue(rig.player().hook == null, "The requested initial or later permission callback ran");
        boolean neighborOnly = action == 3 && mutation >= 9;
        if (neighborOnly) expectedHeld[0].setDamageValue(1);
        h.assertTrue(ItemStack.matches(expectedHeld[0], rig.player().getItemInHand(hand)), "An interrupted operation retains the newer held item and its data");
        h.assertTrue(h.getLevel().getBlockState(rig.plantPos().below()) == expectedSoil[0]
                        && h.getLevel().getBlockState(rig.plantPos()) == expectedCrop[0], "The changed or original target states remain intact");
        if (expectedPlant[0] != null) h.assertTrue(expectedPlant[0].equals(saved(h, (AECropBlockEntity) h.getLevel().getBlockEntity(rig.plantPos()))),
                "An interrupted plot keeps its growth, nutrition, genes and tending credit");
        h.assertTrue(rig.player().getInventory().countItem(AEItems.PULLED_WEEDS.get()) == (neighborOnly ? 1 : 0), "Only an unchanged permitted neighbor may yield weeds");
        h.assertTrue(rig.player().getCooldowns().isOnCooldown(item(action)) == neighborOnly, "Cancelled work adds no cooldown");
        if (action == 3) h.assertTrue(CropWeeds.hasWeeds(h.getLevel(), rig.plantPos().east()) != neighborOnly,
                "Tool or player interruption stops the patch; an independently changed center leaves its permitted neighbor usable");
        h.succeed();
    }
    @GameTest(template = "empty") public static void swappedTrowelCancelsTending(GameTestHelper h) { interrupted(h, 0, 0, false); }
    @GameTest(template = "empty") public static void renamedWoodRakeCancelsRefresh(GameTestHelper h) { interrupted(h, 1, 1, false); }
    @GameTest(template = "empty") public static void changedTrowelDataCancelsTilling(GameTestHelper h) { interrupted(h, 2, 2, false); }
    @GameTest(template = "empty") public static void matchingIronRakeReplacementCancelsPatch(GameTestHelper h) { interrupted(h, 3, 3, false); }
    @GameTest(template = "empty") public static void deathDuringTendingCancelsWork(GameTestHelper h) { interrupted(h, 0, 4, false); }
    @GameTest(template = "empty") public static void spectatorDuringRakingCancelsWork(GameTestHelper h) { interrupted(h, 1, 5, false); }
    @GameTest(template = "empty") public static void revokedBuildDuringTillingCancelsWork(GameTestHelper h) { interrupted(h, 2, 6, false); }
    @GameTest(template = "empty") public static void dimensionChangeDuringAreaRakingCancelsWork(GameTestHelper h) { interrupted(h, 3, 7, false); }
    @GameTest(template = "empty") public static void stoppingCrouchDuringTillingCancelsWork(GameTestHelper h) { interrupted(h, 2, 8, false); }
    @GameTest(template = "empty") public static void stoppingCrouchDuringPatchCannotBecomeSingleRaking(GameTestHelper h) { interrupted(h, 3, 8, false); }
    @GameTest(template = "empty") public static void swappedTrowelAtSoilPermissionCancelsTending(GameTestHelper h) { interrupted(h, 0, 0, true); }
    @GameTest(template = "empty") public static void renamedRakeAtSoilPermissionCancelsRefresh(GameTestHelper h) { interrupted(h, 1, 1, true); }
    @GameTest(template = "empty") public static void changedTrowelAtFinalPermissionCancelsTilling(GameTestHelper h) { interrupted(h, 2, 2, true); }
    @GameTest(template = "empty") public static void replacedRakeAtPlotPermissionStopsPatch(GameTestHelper h) { interrupted(h, 3, 3, true); }
    @GameTest(template = "empty") public static void deathAtPlotPermissionStopsAreaRaking(GameTestHelper h) { interrupted(h, 3, 4, true); }
    @GameTest(template = "empty") public static void dimensionChangeAtSoilPermissionCancelsTending(GameTestHelper h) { interrupted(h, 0, 7, true); }
    @GameTest(template = "empty") public static void replacementCropCannotInheritTending(GameTestHelper h) { interrupted(h, 0, 9, true); }
    @GameTest(template = "empty") public static void replacementCropCannotInheritRakeRefresh(GameTestHelper h) { interrupted(h, 1, 9, true); }
    @GameTest(template = "empty") public static void newerGrowthCannotBeRewoundByTending(GameTestHelper h) { interrupted(h, 0, 10, true); }
    @GameTest(template = "empty") public static void changedSoilCancelsSingleRakeRefresh(GameTestHelper h) { interrupted(h, 1, 11, true); }
    @GameTest(template = "empty") public static void replacementPatchCropIsSkippedAndNeighborRemainsUsable(GameTestHelper h) { interrupted(h, 3, 9, true); }
    @GameTest(template = "empty") public static void newerPatchGrowthIsSkippedAndNeighborRemainsUsable(GameTestHelper h) { interrupted(h, 3, 10, true); }
    @GameTest(template = "empty") public static void removedCropCancelsSingleRakeSoilEdit(GameTestHelper h) { interrupted(h, 1, 12, true); }

    @GameTest(template = "empty")
    public static void deadPlayersCannotUseGardenTools(GameTestHelper h) {
        for (int action = 0; action < 4; action++) {
            var rig = rig(h, action);
            var held = tool(action);
            rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
            var state = h.getLevel().getBlockState(rig.clicked());
            rig.player().setHealth(0);
            use(rig, InteractionHand.OFF_HAND);
            h.assertTrue(h.getLevel().getBlockState(rig.clicked()) == state && held.getDamageValue() == 0
                            && !rig.player().getCooldowns().isOnCooldown(item(action)), "Dead players cannot tend, rake, till or clear patches");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void unrelatedHeldItemsCannotDispatchGardenTools(GameTestHelper h) {
        for (int action = 0; action < 2; action++) {
            var rig = rig(h, action);
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            var state = h.getLevel().getBlockState(rig.plantPos());
            var before = saved(h, rig.plant());
            use(rig, InteractionHand.MAIN_HAND);
            h.assertTrue(h.getLevel().getBlockState(rig.plantPos()) == state && before.equals(saved(h, rig.plant()))
                            && !rig.player().getCooldowns().isOnCooldown(item(action)), "Another held item cannot act as a trowel or rake");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void detachedTrowelContextCannotTendCrop(GameTestHelper h) {
        var rig = rig(h, 0);
        var held = tool(0);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var before = saved(h, rig.plant());
        item(0).useOn(new UseOnContext(h.getLevel(), rig.player(), InteractionHand.MAIN_HAND, held.copy(), hit(rig.clicked())) {});
        h.assertTrue(before.equals(saved(h, rig.plant())) && held.getDamageValue() == 0, "A detached context copy cannot tend the plant");
        h.succeed();
    }
    private static void toolEvent(GameTestHelper h, int mutation) {
        var rig = rig(h, 2);
        var held = tool(2);
        rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
        var expected = new ItemStack[]{held.copy()};
        boolean[] observed = {false};
        Consumer<BlockEvent.BlockToolModificationEvent> listener = event -> {
            if (!event.getPos().equals(rig.clicked()) || event.isSimulated()) return;
            observed[0] = true;
            if (mutation == 0) rig.player().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            else if (mutation == 1) held.set(DataComponents.CUSTOM_NAME, Component.literal("New event name"));
            else rig.player().setHealth(0);
            expected[0] = rig.player().getMainHandItem().copy();
            event.setFinalState(Blocks.FARMLAND.defaultBlockState());
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { use(rig, InteractionHand.MAIN_HAND); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        h.assertTrue(observed[0] && h.getLevel().getBlockState(rig.clicked()).is(Blocks.DIRT)
                        && ItemStack.matches(expected[0], rig.player().getMainHandItem())
                        && !rig.player().getCooldowns().isOnCooldown(item(2)), "Interrupted tilling events preserve dirt, the newer item and cooldown");
        h.succeed();
    }
    @GameTest(template = "empty") public static void toolReplacementDuringTillingEventCancelsWork(GameTestHelper h) { toolEvent(h, 0); }
    @GameTest(template = "empty") public static void toolRenameDuringTillingEventCancelsWork(GameTestHelper h) { toolEvent(h, 1); }
    @GameTest(template = "empty") public static void deathDuringTillingEventCancelsWork(GameTestHelper h) { toolEvent(h, 2); }

    private static void normalUse(GameTestHelper h, int action) {
        var rig = rig(h, action);
        var held = tool(action);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        int nutrition = rig.plant() == null ? 0 : rig.plant().soilNutrition();
        use(rig, InteractionHand.OFF_HAND);
        int damage = action == 3 ? 2 : 1;
        h.assertTrue(held.getDamageValue() == damage && rig.player().getCooldowns().isOnCooldown(item(action)), "Normal work retains its exact durability and cooldown cost");
        if (action == 0) h.assertTrue(rig.plant().hasBeenTended() && rig.plant().soilNutrition() == nutrition + 10
                        && h.getLevel().getBlockState(rig.plantPos()).getValue(AgriCropBlock.AGE) == 4, "Tending adds one growth stage and ten nutrition");
        if (action == 1) h.assertTrue(!CropWeeds.hasWeeds(h.getLevel(), rig.plantPos()) && rig.plant().soilNutrition() == nutrition + 10
                        && h.getLevel().getBlockState(rig.plantPos().below()).getValue(FarmBlock.MOISTURE) == 7
                        && rig.player().getInventory().countItem(AEItems.PULLED_WEEDS.get()) == 1, "Single raking clears weeds, refreshes soil and returns one cutting");
        if (action == 2) h.assertTrue(h.getLevel().getBlockState(rig.clicked()).is(Blocks.FARMLAND), "Normal tilling prepares farmland");
        if (action == 3) h.assertTrue(!CropWeeds.hasWeeds(h.getLevel(), rig.plantPos()) && !CropWeeds.hasWeeds(h.getLevel(), rig.plantPos().east())
                        && rig.player().getInventory().countItem(AEItems.PULLED_WEEDS.get()) == 2 && rig.plant().soilNutrition() == nutrition,
                "Area weeding returns one cutting per plot and preserves nutrition");
        h.succeed();
    }
    @GameTest(template = "empty") public static void currentOffhandTendingKeepsExactEffects(GameTestHelper h) { normalUse(h, 0); }
    @GameTest(template = "empty") public static void currentOffhandRakingKeepsExactEffects(GameTestHelper h) { normalUse(h, 1); }
    @GameTest(template = "empty") public static void currentOffhandTillingKeepsExactEffects(GameTestHelper h) { normalUse(h, 2); }
    @GameTest(template = "empty") public static void currentOffhandAreaRakingKeepsExactEffects(GameTestHelper h) { normalUse(h, 3); }
}
