package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class PortableStorageGameTests {
    private record Storage(Player player, ItemStack bag, AbstractContainerMenu menu, ItemStack contents) {}

    private static ItemStack foodBox(ItemStack food) {
        var box = new ItemStack(AEItems.LUNCH_BOX.get());
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(food)));
        return box;
    }

    private static Storage storage(GameTestHelper h, boolean lunch, GameType mode) {
        var player = h.makeMockPlayer(mode);
        var contents = lunch ? new ItemStack(Items.BREAD, 7) : new ItemStack(AEItems.TOMATO_SEED.get(), 7);
        contents.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Saved supplies"));
        var bag = new ItemStack(lunch ? AEItems.LUNCH_BOX.get() : AEItems.SEED_BAG.get());
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
        player.getInventory().setItem(0, bag);
        return new Storage(player, bag, menu(player, lunch), contents);
    }

    private static AbstractContainerMenu menu(Player player, boolean lunch) {
        return lunch ? new LunchBoxMenu(1, player.getInventory(), 0) : new SeedBagMenu(1, player.getInventory(), 0);
    }

    private static boolean stored(Storage source, ItemStack expected) {
        var contents = source.bag().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList();
        return contents.size() == 1 && ItemStack.matches(contents.getFirst(), expected);
    }

    private static void foreignClicks(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SURVIVAL);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        source.menu().clicked(0, 0, ClickType.PICKUP, other);
        h.assertTrue(source.menu().getCarried().isEmpty() && stored(source, source.contents()),
                "A foreign player cannot pick up another player's portable contents");
        h.assertTrue(source.menu().quickMoveStack(other, 0).isEmpty(), "Foreign shift-clicks are rejected too");
        h.succeed();
    }

    @GameTest(template = "empty") public static void seedBagRejectsForeignClicks(GameTestHelper h) { foreignClicks(h, false); }
    @GameTest(template = "empty") public static void lunchBoxRejectsForeignClicks(GameTestHelper h) { foreignClicks(h, true); }

    private static void spectator(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SPECTATOR);
        h.assertFalse(source.menu().clickMenuButton(source.player(), 0), "Spectators cannot deposit through portable controls");
        source.menu().clicked(0, 0, ClickType.PICKUP, source.player());
        h.assertTrue(source.menu().getCarried().isEmpty() && stored(source, source.contents()), "Spectator pickup preserves stored supplies");
        h.assertTrue(source.menu().quickMoveStack(source.player(), 0).isEmpty(), "Spectator shift-click preserves stored supplies");
        h.succeed();
    }

    @GameTest(template = "empty") public static void seedBagRejectsSpectatorChanges(GameTestHelper h) { spectator(h, false); }
    @GameTest(template = "empty") public static void lunchBoxRejectsSpectatorChanges(GameTestHelper h) { spectator(h, true); }

    private static void dead(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SURVIVAL);
        source.player().setHealth(0);
        h.assertTrue(source.menu().quickMoveStack(source.player(), 0).isEmpty() && stored(source, source.contents()),
                "A dead player's old menu cannot withdraw supplies after death handling");
        h.succeed();
    }

    @GameTest(template = "empty") public static void seedBagStopsAfterDeath(GameTestHelper h) { dead(h, false); }
    @GameTest(template = "empty") public static void lunchBoxStopsAfterDeath(GameTestHelper h) { dead(h, true); }

    private static void closed(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SURVIVAL);
        source.menu().removed(source.player());
        source.player().getInventory().setItem(9, source.contents().copyWithCount(3));
        h.assertFalse(source.menu().clickMenuButton(source.player(), 0), "Closing permanently invalidates old deposit controls");
        h.assertTrue(source.player().getInventory().getItem(9).getCount() == 3 && stored(source, source.contents()),
                "A late deposit leaves player and portable inventories unchanged");
        h.assertTrue(source.menu().quickMoveStack(source.player(), 0).isEmpty(), "A closed menu cannot withdraw either");
        h.succeed();
    }

    @GameTest(template = "empty") public static void closedSeedBagRejectsLateChanges(GameTestHelper h) { closed(h, false); }
    @GameTest(template = "empty") public static void closedLunchBoxRejectsLateChanges(GameTestHelper h) { closed(h, true); }

    private static void replacedContents(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SURVIVAL);
        var replacement = lunch ? new ItemStack(Items.APPLE, 3) : new ItemStack(Items.WHEAT_SEEDS, 3);
        source.bag().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(replacement)));
        source.menu().clicked(0, 0, ClickType.PICKUP, source.player());
        source.menu().removed(source.player());
        h.assertTrue(stored(source, replacement) && source.menu().getCarried().isEmpty(),
                "A menu snapshot cannot overwrite newer contents or withdraw its older copy");
        h.succeed();
    }

    @GameTest(template = "empty") public static void seedBagPreservesNewerContents(GameTestHelper h) { replacedContents(h, false); }
    @GameTest(template = "empty") public static void lunchBoxPreservesNewerContents(GameTestHelper h) { replacedContents(h, true); }

    private static void twoMenus(GameTestHelper h, boolean lunch) {
        var source = storage(h, lunch, GameType.SURVIVAL);
        var newer = menu(source.player(), lunch);
        h.assertFalse(newer.quickMoveStack(source.player(), 0).isEmpty(), "The current contents can be withdrawn normally");
        source.menu().removed(source.player());
        h.assertTrue(source.bag().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().count() == 0,
                "Closing an older snapshot cannot restore supplies withdrawn from a newer menu");
        h.assertTrue(ItemStack.matches(source.player().getInventory().getItem(8), source.contents()),
                "The withdrawn stack retains its full count and name");
        h.succeed();
    }

    @GameTest(template = "empty") public static void seedBagCannotRestoreAnOlderSnapshot(GameTestHelper h) { twoMenus(h, false); }
    @GameTest(template = "empty") public static void lunchBoxCannotRestoreAnOlderSnapshot(GameTestHelper h) { twoMenus(h, true); }

    @GameTest(template = "empty")
    public static void offhandStorageStillWorksInAdventure(GameTestHelper h) {
        for (boolean lunch : new boolean[]{false, true}) {
            var player = h.makeMockPlayer(GameType.ADVENTURE);
            player.getAbilities().mayBuild = false;
            var contents = lunch ? new ItemStack(Items.BREAD, 3) : new ItemStack(AEItems.TOMATO_SEED.get(), 3);
            contents.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Offhand supplies"));
            var bag = new ItemStack(lunch ? AEItems.LUNCH_BOX.get() : AEItems.SEED_BAG.get());
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(contents)));
            player.getInventory().setItem(40, bag);
            AbstractContainerMenu menu = lunch ? new LunchBoxMenu(1, player.getInventory(), 40)
                    : new SeedBagMenu(1, player.getInventory(), 40);
            menu.clicked(0, 40, ClickType.SWAP, player);
            h.assertTrue(player.getOffhandItem() == bag && menu.getSlot(0).getItem().getCount() == 3,
                    "Offhand swapping cannot move the open portable container");
            h.assertFalse(menu.quickMoveStack(player, 0).isEmpty(), "Adventure permits normal personal-inventory withdrawal");
            h.assertTrue(menu.clickMenuButton(player, 0), "Adventure permits normal personal-inventory deposits");
            h.assertTrue(ItemStack.matches(bag.get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow(), contents),
                    "Offhand withdraw and deposit preserve the original named stack");
            menu.removed(player);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchSettingsDoNotInvalidateTheContentsSnapshot(GameTestHelper h) {
        var source = storage(h, true, GameType.SURVIVAL);
        LunchBoxItem.toggle(source.bag());
        h.assertTrue(source.menu().clickMenuButton(source.player(), 3), "Food priorities can change after an external pause toggle");
        h.assertFalse(source.menu().quickMoveStack(source.player(), 0).isEmpty(), "A settings-only change does not block withdrawals");
        h.assertTrue(!LunchBoxItem.enabled(source.bag()) && LunchBoxItem.priority(source.bag(), 0) == 2,
                "Saving contents preserves both pause and priority settings");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchPreviewDoesNotRequireAPlayer(GameTestHelper h) {
        var inventory = new net.minecraft.world.entity.player.Inventory(null);
        var menu = new LunchBoxMenu(1, inventory, -1);
        menu.getSlot(0).set(new ItemStack(Items.BREAD, 3));
        menu.setData(18, 1);
        h.assertTrue(menu.feedingEnabled() && menu.getSlot(0).getItem().getCount() == 3
                        && inventory.isEmpty(),
                "The world-free preview can populate its display without writing to a held item");
        h.succeed();
    }

    private static final class EatingPlayer extends Player {
        Runnable onEat = () -> {};
        ItemStack returned;
        EatingPlayer(GameTestHelper h) {
            super(h.getLevel(), BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "lunch-callback"));
            setPos(h.absolutePos(new BlockPos(4, 220, 4)).getCenter());
            getFoodData().setFoodLevel(0);
        }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return getAbilities().instabuild; }
        @Override public ItemStack eat(Level level, ItemStack serving, FoodProperties properties) {
            onEat.run();
            var result = super.eat(level, serving, properties);
            return returned == null ? result : returned.copy();
        }
    }

    @GameTest(template = "empty")
    public static void foodCallbacksCannotRecursivelyFeedAnotherServing(GameTestHelper h) {
        var player = new EatingPlayer(h);
        var box = foodBox(new ItemStack(Items.BREAD, 3));
        player.getInventory().setItem(0, box);
        int[] callbacks = {0};
        boolean[] nested = {false};
        player.onEat = () -> { if (callbacks[0]++ == 0) nested[0] = LunchBoxItem.feedOne(player); };
        h.assertTrue(LunchBoxItem.feedOne(player), "The initial serving is eaten");
        h.assertTrue(!nested[0] && callbacks[0] == 1 && player.getFoodData().getFoodLevel() == 5
                        && box.get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow().getCount() == 2,
                "A food callback cannot trigger a second serving, even while hunger remains");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void feedingGuardIsScopedToEachPlayer(GameTestHelper h) {
        var first = new EatingPlayer(h);
        var second = new EatingPlayer(h);
        first.getInventory().setItem(0, foodBox(new ItemStack(Items.BREAD, 2)));
        second.getInventory().setItem(0, foodBox(new ItemStack(Items.APPLE, 2)));
        boolean[] fedSecond = {false};
        first.onEat = () -> fedSecond[0] = LunchBoxItem.feedOne(second);
        h.assertTrue(LunchBoxItem.feedOne(first) && fedSecond[0], "Another player's independent serving is allowed during a callback");
        h.assertTrue(first.getFoodData().getFoodLevel() == 5 && second.getFoodData().getFoodLevel() == 4,
                "Each player receives exactly their own meal");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void feedingGuardClearsAfterAFoodCallbackFails(GameTestHelper h) {
        var player = new EatingPlayer(h);
        player.getInventory().setItem(0, foodBox(new ItemStack(Items.BREAD, 2)));
        player.onEat = () -> { throw new IllegalStateException("test food callback"); };
        boolean failed = false;
        try { LunchBoxItem.feedOne(player); }
        catch (IllegalStateException expected) { failed = expected.getMessage().equals("test food callback"); }
        h.assertTrue(failed, "The test exercised the failing food callback");
        player.onEat = () -> {};
        h.assertTrue(LunchBoxItem.feedOne(player) && player.getFoodData().getFoodLevel() == 5,
                "A failed callback cannot leave auto-feeding permanently locked");
        h.succeed();
    }

    private static void fillInventory(EatingPlayer player, ItemStack box) {
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(0, box);
        player.getInventory().setItem(40, new ItemStack(Items.DIRT, 64));
    }

    private static int droppedBowls(EatingPlayer player) {
        return player.level().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(3)).stream()
                .filter(e -> e.getItem().is(Items.BOWL)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    @GameTest(template = "empty")
    public static void partialFoodContainerReturnsDropTheOverflow(GameTestHelper h) {
        var player = new EatingPlayer(h);
        fillInventory(player, foodBox(new ItemStack(Items.BREAD)));
        player.getInventory().setItem(1, new ItemStack(Items.BOWL, 63));
        player.returned = new ItemStack(Items.BOWL, 4);
        h.assertTrue(LunchBoxItem.feedOne(player), "The custom food return is processed");
        h.assertTrue(player.getInventory().getItem(1).getCount() == 64 && droppedBowls(player) == 3,
                "A partly accepted container stack keeps every remaining bowl as a world drop");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foodContainerReturnsSurviveCreativeChanges(GameTestHelper h) {
        var player = new EatingPlayer(h);
        fillInventory(player, foodBox(new ItemStack(Items.BREAD)));
        player.returned = new ItemStack(Items.BOWL);
        player.onEat = () -> player.getAbilities().instabuild = true;
        h.assertTrue(LunchBoxItem.feedOne(player), "The serving started in Survival");
        h.assertTrue(droppedBowls(player) == 1, "A Creative change during eating cannot silently discard the container");
        h.succeed();
    }
}
