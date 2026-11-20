package com.agrievolution.content;

import com.agrievolution.farming.BotanicalGreenhouseLayout;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class BotanicalInteractionSafetyGameTests {
    private static final class HookPlayer extends Player {
        BlockPos trigger;
        Runnable hook;
        HookPlayer(ServerLevel level, BlockPos pos) {
            super(level, pos, 0, new GameProfile(UUID.randomUUID(), "botanical-click"));
            setPos(Vec3.atCenterOf(pos));
            getAbilities().mayBuild = true;
        }
        @Override public boolean isCreative() { return false; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean mayUseItemAt(BlockPos pos, Direction side, ItemStack stack) {
            if (pos.equals(trigger) && hook != null) {
                var callback = hook;
                hook = null;
                callback.run();
            }
            return super.mayUseItemAt(pos, side, stack);
        }
    }
    private record Rig(BlockPos pos, BotanicalGreenhouseBlockEntity entity, HookPlayer player) {}
    private static Rig rig(GameTestHelper h, int action) {
        var pos = h.absolutePos(new BlockPos(4, 180 + action * 5, 4));
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState().setValue(BotanicalGreenhouseBlock.FACING, Direction.NORTH), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(greenhouse.addWater(1000), "The controller starts with one stored bucket");
        if (action == 1) h.assertTrue(greenhouse.installAutomationUpgrade(), "Prepare the prerequisite for Smart Farming");
        return new Rig(pos, greenhouse, new HookPlayer(h.getLevel(), pos));
    }
    private static ItemStack source(int action) {
        return new ItemStack(switch (action) {
            case 0 -> AEItems.GREENHOUSE_AUTOMATION_UPGRADE.get();
            case 1 -> AEItems.GREENHOUSE_SMART_FARMING_UPGRADE.get();
            case 2 -> Items.COAL;
            case 3 -> Items.CHARCOAL;
            case 4 -> Items.WATER_BUCKET;
            case 5 -> Items.BUCKET;
            default -> Items.OAK_DOOR;
        }, action == 4 ? 1 : 3);
    }
    private static void use(GameTestHelper h, Rig rig, InteractionHand hand) {
        AEBlocks.BOTANICAL_GREENHOUSE.get().useItemOn(rig.player().getItemInHand(hand), h.getLevel().getBlockState(rig.pos()),
                h.getLevel(), rig.pos(), rig.player(), hand, new BlockHitResult(Vec3.atCenterOf(rig.pos()), Direction.UP, rig.pos(), false));
    }
    private static CompoundTag saved(GameTestHelper h, BotanicalGreenhouseBlockEntity greenhouse) {
        var tag = new CompoundTag();
        greenhouse.saveAdditional(tag, h.getLevel().registryAccess());
        return tag;
    }
    private static void replace(GameTestHelper h, Rig rig) {
        var tag = saved(h, rig.entity());
        h.getLevel().removeBlockEntity(rig.pos());
        var current = new BotanicalGreenhouseBlockEntity(rig.pos(), h.getLevel().getBlockState(rig.pos()));
        current.loadAdditional(tag, h.getLevel().registryAccess());
        h.getLevel().setBlockEntity(current);
    }

    private static void interrupted(GameTestHelper h, int action, int mutation) {
        var rig = rig(h, action);
        var hand = action % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        var held = source(action);
        rig.player().setItemInHand(hand, held);
        var expectedHeld = new ItemStack[]{held.copy()};
        var before = saved(h, rig.entity());
        rig.player().trigger = rig.pos();
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(hand, new ItemStack(Items.STICK, 4));
                case 1 -> held.set(DataComponents.CUSTOM_NAME, Component.literal("Changed during permission check"));
                case 2 -> held.grow(1);
                case 3 -> rig.player().setItemInHand(hand, held.copy());
                case 4 -> rig.player().setHealth(0);
                case 5 -> replace(h, rig);
                case 6 -> h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(BotanicalGreenhouseBlock.FACING, Direction.EAST), 18);
                case 7 -> rig.player().setShiftKeyDown(true);
                default -> throw new AssertionError("Unknown callback mutation");
            }
            expectedHeld[0] = rig.player().getItemInHand(hand).copy();
        };
        use(h, rig, hand);
        var current = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(rig.pos());
        h.assertTrue(before.equals(saved(h, current)), "An interrupted resource click preserves water, heater fuel, upgrades and inventories");
        h.assertTrue(ItemStack.matches(expectedHeld[0], rig.player().getItemInHand(hand)), "The callback's held item, count and saved data are not consumed or overwritten");
        if (mutation == 6) h.assertTrue(h.getLevel().getBlockState(rig.pos()).getValue(BotanicalGreenhouseBlock.FACING) == Direction.EAST, "The callback's new controller facing is preserved");
        h.succeed();
    }
    @GameTest(template = "empty") public static void replacedAutomationModuleCancelsBotanicalUpgrade(GameTestHelper h) { interrupted(h, 0, 0); }
    @GameTest(template = "empty") public static void replacedSmartModuleCancelsBotanicalUpgrade(GameTestHelper h) { interrupted(h, 1, 0); }
    @GameTest(template = "empty") public static void replacedCoalCancelsBotanicalFuel(GameTestHelper h) { interrupted(h, 2, 0); }
    @GameTest(template = "empty") public static void changedCharcoalDataCancelsBotanicalFuel(GameTestHelper h) { interrupted(h, 3, 1); }
    @GameTest(template = "empty") public static void replacedWaterBucketCancelsBotanicalFill(GameTestHelper h) { interrupted(h, 4, 0); }
    @GameTest(template = "empty") public static void replacedEmptyBucketCancelsBotanicalDrain(GameTestHelper h) { interrupted(h, 5, 0); }
    @GameTest(template = "empty") public static void changedAutomationModuleDataCancelsBotanicalUpgrade(GameTestHelper h) { interrupted(h, 0, 1); }
    @GameTest(template = "empty") public static void changedSmartModuleCountCancelsBotanicalUpgrade(GameTestHelper h) { interrupted(h, 1, 2); }
    @GameTest(template = "empty") public static void changedCoalCountCancelsBotanicalFuel(GameTestHelper h) { interrupted(h, 2, 2); }
    @GameTest(template = "empty") public static void changedBucketDataCancelsBotanicalFill(GameTestHelper h) { interrupted(h, 4, 1); }
    @GameTest(template = "empty") public static void matchingReplacementBucketCancelsBotanicalDrain(GameTestHelper h) { interrupted(h, 5, 3); }
    @GameTest(template = "empty") public static void replacementBotanicalControllerCannotInheritFill(GameTestHelper h) { interrupted(h, 4, 5); }
    @GameTest(template = "empty") public static void replacementBotanicalControllerCannotInheritAutomation(GameTestHelper h) { interrupted(h, 0, 5); }
    @GameTest(template = "empty") public static void replacementBotanicalControllerCannotInheritSmartUpgrade(GameTestHelper h) { interrupted(h, 1, 5); }
    @GameTest(template = "empty") public static void replacementBotanicalControllerCannotInheritFuel(GameTestHelper h) { interrupted(h, 2, 5); }
    @GameTest(template = "empty") public static void rotatedBotanicalControllerCancelsPendingFill(GameTestHelper h) { interrupted(h, 4, 6); }
    @GameTest(template = "empty") public static void crouchingDuringBotanicalFuelCancelsClick(GameTestHelper h) { interrupted(h, 2, 7); }
    @GameTest(template = "empty") public static void deathDuringBotanicalUpgradeCancelsClick(GameTestHelper h) { interrupted(h, 0, 4); }

    @GameTest(template = "empty")
    public static void deadPlayerCannotUseBotanicalResources(GameTestHelper h) {
        for (int action = 0; action < 6; action++) {
            var rig = rig(h, action);
            var held = source(action);
            rig.player().setItemInHand(InteractionHand.MAIN_HAND, held);
            var expected = held.copy();
            var before = saved(h, rig.entity());
            rig.player().setHealth(0);
            use(h, rig, InteractionHand.MAIN_HAND);
            h.assertTrue(before.equals(saved(h, rig.entity())) && ItemStack.matches(expected, rig.player().getMainHandItem()),
                    "Dead players cannot install either module, fuel the heater, fill or drain water");
        }
        h.succeed();
    }

    private static BlockPos doorway(GameTestHelper h, Rig rig, boolean floor) {
        var lower = BotanicalGreenhouseLayout.at(rig.pos(), Direction.NORTH, 1, 0, 0);
        if (floor) h.getLevel().setBlock(lower.below(), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(lower, Blocks.BLUE_STAINED_GLASS.defaultBlockState(), 3);
        h.getLevel().setBlock(lower.above(), Blocks.GLASS_PANE.defaultBlockState(), 3);
        return lower;
    }
    private static void interruptedDoor(GameTestHelper h, int mutation) {
        var rig = rig(h, 6);
        var lower = doorway(h, rig, true);
        var held = source(6);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        var expected = new ItemStack[]{held.copy()};
        rig.player().trigger = mutation % 2 == 0 ? lower.above() : lower;
        rig.player().hook = () -> {
            switch (mutation) {
                case 0 -> rig.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 4));
                case 1 -> held.set(DataComponents.CUSTOM_NAME, Component.literal("Changed doorway item"));
                case 2 -> held.grow(1);
                case 3 -> replace(h, rig);
                case 4 -> h.getLevel().setBlock(rig.pos(), h.getLevel().getBlockState(rig.pos()).setValue(BotanicalGreenhouseBlock.FACING, Direction.EAST), 18);
                case 5 -> h.getLevel().setBlock(lower, Blocks.RED_STAINED_GLASS.defaultBlockState(), 3);
                case 6 -> h.getLevel().setBlock(lower.above(), Blocks.BLACK_STAINED_GLASS_PANE.defaultBlockState(), 3);
                case 7 -> rig.player().setShiftKeyDown(true);
                default -> throw new AssertionError("Unknown doorway mutation");
            }
            expected[0] = rig.player().getOffhandItem().copy();
        };
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(h.getLevel().getBlockState(lower).is(mutation == 5 ? Blocks.RED_STAINED_GLASS : Blocks.BLUE_STAINED_GLASS)
                        && h.getLevel().getBlockState(lower.above()).is(mutation == 6 ? Blocks.BLACK_STAINED_GLASS_PANE : Blocks.GLASS_PANE),
                "Interrupted doorway permissions leave both original or newly changed wall blocks in place");
        h.assertTrue(ItemStack.matches(expected[0], rig.player().getOffhandItem()) && rig.player().getInventory().countItem(Items.BLUE_STAINED_GLASS) == 0,
                "An interrupted door install spends no item and issues no glass refund");
        h.succeed();
    }
    @GameTest(template = "empty") public static void replacedDoorDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 0); }
    @GameTest(template = "empty") public static void changedDoorDataDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 1); }
    @GameTest(template = "empty") public static void changedDoorCountDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 2); }
    @GameTest(template = "empty") public static void replacementControllerDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 3); }
    @GameTest(template = "empty") public static void rotatedControllerDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 4); }
    @GameTest(template = "empty") public static void changedLowerGlassDuringPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 5); }
    @GameTest(template = "empty") public static void changedUpperGlassDuringPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 6); }
    @GameTest(template = "empty") public static void crouchingDuringWallPermissionCancelsInstall(GameTestHelper h) { interruptedDoor(h, 7); }

    private static void normalResources(GameTestHelper h, boolean creative) {
        var rig = rig(h, 0);
        rig.player().getAbilities().instabuild = creative;
        for (int action = 0; action < 6; action++) {
            var hand = action % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            var held = source(action);
            rig.player().setItemInHand(hand, held);
            use(h, rig, hand);
            if (creative) h.assertTrue(rig.player().getItemInHand(hand) == held && held.getCount() == (action == 4 ? 1 : 3), "Creative resource interactions retain the held stack");
            else if (action < 4) h.assertTrue(held.getCount() == 2, "Each successful module or heater action spends one item");
        }
        h.assertTrue(rig.entity().isAutomationUpgraded() && rig.entity().isSmartFarmingUpgraded()
                        && rig.entity().data.get(0) == 1000 && rig.entity().data.get(16) == 1200,
                "Both upgrades, coal, charcoal and both water directions retain their normal effects and costs");
        if (!creative) h.assertTrue(rig.player().getOffhandItem().is(Items.BUCKET) && rig.player().getOffhandItem().getCount() == 2
                        && rig.player().getInventory().countItem(Items.WATER_BUCKET) == 1, "Stacked collection retains two spare empty buckets and returns one filled bucket");
        h.succeed();
    }
    @GameTest(template = "empty") public static void currentBotanicalResourceClicksKeepExactCosts(GameTestHelper h) { normalResources(h, false); }
    @GameTest(template = "empty") public static void creativeBotanicalResourceClicksKeepHeldItems(GameTestHelper h) { normalResources(h, true); }

    @GameTest(template = "empty")
    public static void currentOffhandDoorInstallKeepsExactRefund(GameTestHelper h) {
        var rig = rig(h, 6);
        doorway(h, rig, true);
        var held = source(6);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(BotanicalGreenhouseLayout.hasValidDoor(h.getLevel(), rig.pos(), Direction.NORTH) && held.getCount() == 2
                        && rig.player().getInventory().countItem(Items.BLUE_STAINED_GLASS) == 1 && rig.player().getInventory().countItem(Items.GLASS_PANE) == 1,
                "A normal door retrofit consumes one door and returns each removed glass block once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unsupportedBotanicalDoorRestoresWallAndItem(GameTestHelper h) {
        var rig = rig(h, 6);
        var lower = doorway(h, rig, false);
        var held = source(6);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(h.getLevel().getBlockState(lower).is(Blocks.BLUE_STAINED_GLASS) && h.getLevel().getBlockState(lower.above()).is(Blocks.GLASS_PANE)
                        && ItemStack.matches(source(6), rig.player().getOffhandItem()), "A genuine placement failure restores the two glass states and unspent door stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smartBotanicalUpgradeStillRequiresAutomation(GameTestHelper h) {
        var rig = rig(h, 0);
        var held = source(1);
        rig.player().setItemInHand(InteractionHand.OFF_HAND, held);
        use(h, rig, InteractionHand.OFF_HAND);
        h.assertTrue(!rig.entity().isAutomationUpgraded() && !rig.entity().isSmartFarmingUpgraded() && held.getCount() == 3,
                "Smart Farming cannot skip the automation prerequisite or charge a failed module");
        h.succeed();
    }
}
