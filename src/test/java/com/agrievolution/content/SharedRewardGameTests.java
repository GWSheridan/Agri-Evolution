package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class SharedRewardGameTests {
    private static final class CallbackInventory extends Inventory {
        Runnable beforeAdd = () -> {};
        int limit = 64;
        boolean returnFalse;
        CallbackInventory(Player player) { super(player); }
        @Override public boolean add(int slot, ItemStack payment) {
            var callback = beforeAdd;
            beforeAdd = () -> {};
            callback.run();
            int offered = Math.min(limit, payment.getCount());
            var part = payment.copyWithCount(offered);
            boolean added = super.add(slot, part);
            payment.shrink(offered - part.getCount());
            return !returnFalse && added;
        }
    }

    private static final class CallbackPlayer extends Player {
        private final CallbackInventory callbacks;
        CallbackPlayer(ServerLevel level) {
            super(level, BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "market-callback"));
            callbacks = new CallbackInventory(this);
        }
        @Override public Inventory getInventory() { return callbacks == null ? super.getInventory() : callbacks; }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }

    private static SharedMarketOrders funded(GameTestHelper h, Player player, int amount) {
        var data = new CompoundTag();
        var row = new CompoundTag();
        var member = new CompoundTag();
        member.putUUID("Player", player.getUUID());
        member.putInt("Count", amount);
        var rewards = new ListTag();
        rewards.add(member);
        row.put("Rewards", rewards);
        data.put("Order0", row);
        return SharedMarketOrders.load(data, h.getLevel().registryAccess());
    }

    private static void supplyOrder(Player player) {
        player.getInventory().setItem(9, new ItemStack(AEItems.TOMATO.get(), 32));
        player.getInventory().setItem(10, new ItemStack(AEItems.LETTUCE.get(), 24));
        player.getInventory().setItem(11, new ItemStack(AEItems.CUCUMBER.get(), 24));
    }

    @GameTest(template = "empty")
    public static void rewardCallbackCannotClaimTheSameBalanceTwice(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        boolean[] nested = {true};
        player.callbacks.beforeAdd = () -> nested[0] = orders.claim(0, player);
        h.assertTrue(orders.claim(0, player), "The original reward is delivered");
        h.assertTrue(!nested[0] && player.getInventory().countItem(Items.EMERALD) == 5
                        && orders.reward(0, player.getUUID()) == 0,
                "An inventory callback cannot claim the same five emeralds again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rewardLedgerIsSavedBeforeDeliveryCallbacks(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        int[] savedDue = {-1};
        player.callbacks.beforeAdd = () -> savedDue[0] = SharedMarketOrders.load(
                orders.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess()).reward(0, player.getUUID());
        orders.claim(0, player);
        h.assertTrue(savedDue[0] == 0 && player.getInventory().countItem(Items.EMERALD) == 5,
                "A save during item delivery does not retain the reward that is already being paid");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rewardsEarnedDuringAClaimRemainAvailable(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        supplyOrder(player);
        player.callbacks.beforeAdd = () -> h.assertTrue(orders.contribute(0, 0, player), "The callback completes a new order round");
        h.assertTrue(orders.claim(0, player) && player.getInventory().countItem(Items.EMERALD) == 5
                        && orders.reward(0, player.getUUID()) == 6,
                "Finishing the old claim cannot overwrite the six emeralds earned during delivery");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void partiallyAcceptedRewardChargesOnlyTheInsertedAmount(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        player.callbacks.limit = 1;
        player.callbacks.returnFalse = true;
        h.assertTrue(orders.claim(0, player) && player.getInventory().countItem(Items.EMERALD) == 1
                        && orders.reward(0, player.getUUID()) == 4,
                "A partial insertion with a false result still charges exactly the inserted emerald");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void consumedPaymentCannotRemainClaimableAfterAFalseResult(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 3);
        player.callbacks.returnFalse = true;
        h.assertTrue(orders.claim(0, player) && player.getInventory().countItem(Items.EMERALD) == 3
                        && orders.reward(0, player.getUUID()) == 0,
                "A consumed payment is charged even when an inventory extension returns false");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectedRewardRemainderMergesWithNewEarnings(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        supplyOrder(player);
        player.callbacks.limit = 1;
        player.callbacks.returnFalse = true;
        player.callbacks.beforeAdd = () -> orders.contribute(0, 0, player);
        h.assertTrue(orders.claim(0, player) && player.getInventory().countItem(Items.EMERALD) == 1
                        && orders.reward(0, player.getUUID()) == 10,
                "Four rejected emeralds return alongside all six newly earned emeralds");
        h.succeed();
    }

    private static void deniedClaim(GameTestHelper h, boolean dead) {
        var player = h.makeMockPlayer(dead ? GameType.SURVIVAL : GameType.SPECTATOR);
        if (dead) player.setHealth(0);
        var orders = funded(h, player, 5);
        h.assertTrue(!orders.claim(0, player) && player.getInventory().countItem(Items.EMERALD) == 0
                        && orders.reward(0, player.getUUID()) == 5,
                "Inactive players cannot collect rewards through a direct call");
        h.succeed();
    }

    @GameTest(template = "empty") public static void spectatorCannotDirectlyClaimRewards(GameTestHelper h) { deniedClaim(h, false); }
    @GameTest(template = "empty") public static void deadPlayerCannotDirectlyClaimRewards(GameTestHelper h) { deniedClaim(h, true); }

    private static void deniedContribution(GameTestHelper h, boolean dead) {
        var player = h.makeMockPlayer(dead ? GameType.SURVIVAL : GameType.SPECTATOR);
        if (dead) player.setHealth(0);
        var orders = new SharedMarketOrders();
        supplyOrder(player);
        h.assertTrue(!orders.contribute(0, 0, player) && orders.round(0) == 0
                        && player.getInventory().getItem(9).getCount() == 32
                        && orders.reward(0, player.getUUID()) == 0,
                "Inactive deliveries leave the inventory and shared order unchanged");
        h.succeed();
    }

    @GameTest(template = "empty") public static void spectatorCannotDirectlyContribute(GameTestHelper h) { deniedContribution(h, false); }
    @GameTest(template = "empty") public static void deadPlayerCannotDirectlyContribute(GameTestHelper h) { deniedContribution(h, true); }

    private record Board(Player player, BlockPos pos, GardenWorkMenu menu) {}
    private static Board board(GameTestHelper h, GameType mode) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), 2);
        var player = h.makeMockPlayer(mode);
        player.setPos(Vec3.atCenterOf(pos));
        return new Board(player, pos, new GardenWorkMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos), false));
    }

    @GameTest(template = "empty")
    public static void closedBoardRejectsLatePageAndDeliveryClicks(GameTestHelper h) {
        var board = board(h, GameType.SURVIVAL);
        supplyOrder(board.player());
        board.menu().removed(board.player());
        h.assertFalse(board.menu().clickMenuButton(board.player(), board.menu().pageAction(GardenWorkMenu.SHARED_PAGE)),
                "A closed screen cannot change pages or reopen its old controls");
        h.assertTrue(board.player().getInventory().getItem(9).getCount() == 32, "Closing leaves delivery items intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignPlayerCannotPickUpBoardInventory(GameTestHelper h) {
        var board = board(h, GameType.SURVIVAL);
        board.player().getInventory().setItem(9, new ItemStack(Items.BREAD, 3));
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(Vec3.atCenterOf(board.pos()));
        board.menu().clicked(0, 0, ClickType.PICKUP, other);
        h.assertTrue(board.menu().getCarried().isEmpty() && board.player().getInventory().getItem(9).getCount() == 3,
                "A board menu cannot expose its owner's inventory to a foreign click");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedBoardRejectsInventoryShiftClicks(GameTestHelper h) {
        var board = board(h, GameType.SURVIVAL);
        board.player().getInventory().setItem(9, new ItemStack(Items.BREAD, 3));
        h.getLevel().setBlock(board.pos(), Blocks.STONE.defaultBlockState(), 2);
        h.assertTrue(board.menu().quickMoveStack(board.player(), 0).isEmpty()
                        && board.player().getInventory().getItem(9).getCount() == 3,
                "Removing the board invalidates its old inventory transfer path");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotPickUpBoardInventory(GameTestHelper h) {
        var board = board(h, GameType.SPECTATOR);
        board.player().getInventory().setItem(9, new ItemStack(Items.BREAD, 3));
        board.menu().clicked(0, 0, ClickType.PICKUP, board.player());
        h.assertTrue(board.menu().getCarried().isEmpty() && board.player().getInventory().getItem(9).getCount() == 3,
                "Spectator board clicks cannot move stored player items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void boardAccessRejectsAnotherDimensionAtTheSameCoordinates(GameTestHelper h) {
        var board = board(h, GameType.SURVIVAL);
        var otherWorld = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(otherWorld != null && otherWorld != h.getLevel(), "The fixture has a separate dimension");
        var player = new CallbackPlayer(otherWorld);
        player.setPos(Vec3.atCenterOf(board.pos()));
        var menu = new GardenWorkMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), board.pos()), false);
        h.assertFalse(menu.stillValid(player), "Matching coordinates in another dimension cannot satisfy board access");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void claimGuardAllowsIndependentPlayers(GameTestHelper h) {
        var first = new CallbackPlayer(h.getLevel());
        var second = new CallbackPlayer(h.getLevel());
        var orders = funded(h, first, 5);
        supplyOrder(second);
        orders.contribute(0, 0, second);
        boolean[] paidSecond = {false};
        first.callbacks.beforeAdd = () -> paidSecond[0] = orders.claim(0, second);
        h.assertTrue(orders.claim(0, first) && paidSecond[0] && first.getInventory().countItem(Items.EMERALD) == 5
                        && second.getInventory().countItem(Items.EMERALD) == 6,
                "A claim in progress does not block a different contributor's independent reward");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void failedClaimRefundsTheBalanceAndClearsItsGuard(GameTestHelper h) {
        var player = new CallbackPlayer(h.getLevel());
        var orders = funded(h, player, 5);
        player.callbacks.beforeAdd = () -> { throw new IllegalStateException("test inventory failure"); };
        boolean failed = false;
        try { orders.claim(0, player); }
        catch (IllegalStateException expected) { failed = expected.getMessage().equals("test inventory failure"); }
        h.assertTrue(failed && orders.reward(0, player.getUUID()) == 5 && orders.claim(0, player)
                        && player.getInventory().countItem(Items.EMERALD) == 5,
                "An insertion failure restores unpaid rewards and permits a later normal claim");
        h.succeed();
    }
}
