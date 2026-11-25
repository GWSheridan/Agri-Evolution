package com.agrievolution.content;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class LunchBoxSafetyGameTests {
    private static final ResourceLocation FOOD = ResourceLocation.fromNamespaceAndPath("agri_evolution", "test_lunch_food");
    private static final Map<Component, Probe> PROBES = new HashMap<>();

    private static final class Probe {
        int calls;
        final int trigger;
        final BiConsumer<ItemStack, LivingEntity> callback;
        Probe(int trigger, BiConsumer<ItemStack, LivingEntity> callback) { this.trigger = trigger; this.callback = callback; }
    }

    @EventBusSubscriber(modid = "agri_evolution", bus = EventBusSubscriber.Bus.MOD)
    public static final class TestFoodRegistry {
        @SubscribeEvent public static void register(RegisterEvent event) {
            event.register(Registries.ITEM, FOOD, () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(4).saturationModifier(.3f).usingConvertsTo(Items.BOWL).build())) {
                @Override public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
                    var food = stack.get(DataComponents.FOOD);
                    var key = stack.get(DataComponents.CUSTOM_NAME);
                    var probe = PROBES.get(key);
                    if (probe != null && ++probe.calls == probe.trigger) {
                        PROBES.remove(key);
                        probe.callback.accept(stack, entity);
                    }
                    return food;
                }
            });
        }
    }

    private static final class LunchPlayer extends Player {
        boolean spectator, using;
        LunchPlayer(GameTestHelper h) {
            super(h.getLevel(), BlockPos.ZERO, 0, new GameProfile(UUID.randomUUID(), "lunch-inspection"));
            setPos(h.absolutePos(new BlockPos(4, 220, 4)).getCenter());
            getFoodData().setFoodLevel(0);
            getFoodData().setSaturation(0);
        }
        @Override public boolean isSpectator() { return spectator; }
        @Override public boolean isCreative() { return getAbilities().instabuild; }
        @Override public boolean isUsingItem() { return using || super.isUsingItem(); }
        void changeLevel(Level level) { setLevel(level); }
    }

    private record Lunch(LunchPlayer player, ItemStack box, ItemStack food) {}

    private static ItemStack box(ItemStack... foods) {
        var box = new ItemStack(AEItems.LUNCH_BOX.get());
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(foods)));
        return box;
    }

    private static Lunch lunch(GameTestHelper h) {
        var food = new ItemStack(BuiltInRegistries.ITEM.get(FOOD), 3);
        food.set(DataComponents.CUSTOM_NAME, Component.literal("Packed meal " + UUID.randomUUID()));
        var box = box(food);
        var player = new LunchPlayer(h);
        player.getInventory().setItem(0, box);
        return new Lunch(player, box, food);
    }

    private static boolean feed(Lunch lunch, int query, BiConsumer<ItemStack, LivingEntity> callback) {
        var key = lunch.food().get(DataComponents.CUSTOM_NAME);
        PROBES.put(key, new Probe(query, callback));
        try { return LunchBoxItem.feedOne(lunch.player()); }
        finally { PROBES.remove(key); }
    }

    private static int portions(ItemStack box) {
        return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().mapToInt(ItemStack::getCount).sum();
    }

    private static void notEaten(GameTestHelper h, Lunch lunch) {
        h.assertTrue(portions(lunch.box()) == 3 && lunch.player().getInventory().countItem(Items.BOWL) == 0,
                "Cancelled inspection keeps all packed portions and creates no returned bowl");
    }

    @GameTest(template = "empty")
    public static void replacingLunchBoxDuringSelectionCancelsFeeding(GameTestHelper h) {
        var lunch = lunch(h);
        var replacement = box(new ItemStack(Items.APPLE, 7));
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().getInventory().setItem(0, replacement)),
                "A replaced box cannot be charged or feed the player through its old reference");
        notEaten(h, lunch);
        h.assertTrue(portions(replacement) == 7 && lunch.player().getFoodData().getFoodLevel() == 0, "The replacement and hunger remain unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void movingLunchBoxDuringSelectionCancelsOldAttempt(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> {
            lunch.player().getInventory().setItem(0, ItemStack.EMPTY);
            lunch.player().getInventory().setItem(40, lunch.box());
        }), "Moving a box to the offhand cancels its old inventory-slot attempt");
        notEaten(h, lunch);
        h.assertTrue(LunchBoxItem.feedOne(lunch.player()) && portions(lunch.box()) == 2, "A later attempt can use the box from its new offhand slot");
        h.succeed();
    }

    private static void changedContents(GameTestHelper h, int query) {
        var lunch = lunch(h);
        var replacement = ItemContainerContents.fromItems(List.of(new ItemStack(Items.APPLE, 7)));
        h.assertFalse(feed(lunch, query, (stack, player) -> lunch.box().set(DataComponents.CONTAINER, replacement)),
                "Changed contents cancel the old serving before consumption");
        h.assertTrue(lunch.box().get(DataComponents.CONTAINER).equals(replacement)
                        && lunch.player().getFoodData().getFoodLevel() == 0, "The newer food contents are preserved exactly");
        h.succeed();
    }

    @GameTest(template = "empty") public static void lunchSelectionPreservesReplacedContents(GameTestHelper h) { changedContents(h, 1); }
    @GameTest(template = "empty") public static void lunchFinalFoodQueryPreservesReplacedContents(GameTestHelper h) { changedContents(h, 2); }

    @GameTest(template = "empty")
    public static void pausingLunchDuringInspectionStopsTheServing(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> LunchBoxItem.toggle(lunch.box())), "Pausing during food inspection takes effect before the serving");
        notEaten(h, lunch);
        h.assertFalse(LunchBoxItem.enabled(lunch.box()), "The pause setting is retained");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changingLunchPrioritiesCancelsOldSelection(GameTestHelper h) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(lunch.food(), new ItemStack(Items.APPLE, 4))));
        h.assertFalse(feed(lunch, 1, (stack, player) -> LunchBoxItem.cyclePriority(lunch.box(), 1)), "Changed food priorities cancel an inspection in progress");
        h.assertTrue(portions(lunch.box()) == 7 && lunch.player().getFoodData().getFoodLevel() == 0, "Neither food stack was charged");
        h.assertTrue(LunchBoxItem.feedOne(lunch.player()) && lunch.player().getInventory().countItem(Items.BOWL) == 0,
                "The next attempt follows the new higher-priority apple slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void protectingLunchDuringFinalQueryStopsTheServing(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 2, (stack, player) -> LunchBoxItem.toggleProtected(lunch.box(), 0)), "A newly protected slot cannot be consumed by an earlier selection");
        notEaten(h, lunch);
        h.assertTrue(LunchBoxItem.protectedSlot(lunch.box(), 0), "The new protection setting is retained");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void renamedLunchBoxIsNotOverwrittenByOldInspection(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.box().set(DataComponents.CUSTOM_NAME, Component.literal("Renamed box"))),
                "A component change invalidates the old box snapshot");
        notEaten(h, lunch);
        h.assertTrue(Component.literal("Renamed box").equals(lunch.box().get(DataComponents.CUSTOM_NAME)), "The changed box name is preserved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchInspectionStopsWhenHungerBecomesFull(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().getFoodData().setFoodLevel(20)), "A player filled by a callback does not spend another serving");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchFinalQueryCannotFeedAfterDeath(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 2, (stack, player) -> lunch.player().setHealth(0)), "Death during inspection cancels the serving before its food effects");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchInspectionStopsAfterCreativeChange(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().getAbilities().instabuild = true), "A Creative change during inspection cancels Survival feeding");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lunchInspectionStopsAfterSpectatorChange(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().spectator = true), "Entering Spectator during inspection cancels the serving");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void openingLunchMenuDuringInspectionPausesFeeding(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().containerMenu = new LunchBoxMenu(1, lunch.player().getInventory(), 0)),
                "Opening a menu during inspection pauses auto-feeding before consumption");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void startingItemUseDuringLunchInspectionPausesFeeding(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().using = true), "Starting another item use during inspection pauses auto-feeding");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changingDimensionDuringLunchInspectionCancelsFeeding(GameTestHelper h) {
        var lunch = lunch(h);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null, "The Nether exists for the dimension change check");
        try {
            h.assertFalse(feed(lunch, 1, (stack, player) -> lunch.player().changeLevel(other)), "A dimension change cancels the old world's feeding attempt");
            notEaten(h, lunch);
        } finally { lunch.player().changeLevel(h.getLevel()); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foodQueryCannotRewriteDetachedLunchPortions(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertFalse(feed(lunch, 1, (stack, player) -> stack.setCount(50)), "A food callback cannot change a detached snapshot into extra stored portions");
        notEaten(h, lunch);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recursiveFoodInspectionStillConsumesOneServing(GameTestHelper h) {
        var lunch = lunch(h);
        boolean[] nested = {true};
        h.assertTrue(feed(lunch, 1, (stack, player) -> nested[0] = LunchBoxItem.feedOne(lunch.player())), "The outer serving succeeds");
        h.assertTrue(!nested[0] && portions(lunch.box()) == 2 && lunch.player().getFoodData().getFoodLevel() == 4
                        && lunch.player().getInventory().countItem(Items.BOWL) == 1, "Food inspection cannot recursively charge or feed a second serving");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void protectedLunchFoodIsNotQueriedForAutoFeeding(GameTestHelper h) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(lunch.food(), new ItemStack(Items.BREAD, 2))));
        LunchBoxItem.toggleProtected(lunch.box(), 0);
        h.assertTrue(feed(lunch, 1, (stack, player) -> { throw new AssertionError("Protected food must be skipped before querying modded food properties"); }),
                "The unprotected bread remains usable without invoking the protected food");
        h.assertTrue(portions(lunch.box()) == 4 && lunch.player().getFoodData().getFoodLevel() == 5, "Only one bread is consumed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryModdedLunchFoodKeepsComponentsAndReturns(GameTestHelper h) {
        var lunch = lunch(h);
        h.assertTrue(LunchBoxItem.feedOne(lunch.player()), "Ordinary modded food is consumed through its normal item implementation");
        var remaining = lunch.box().get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow();
        h.assertTrue(remaining.getCount() == 2 && ItemStack.isSameItemSameComponents(remaining, lunch.food())
                        && lunch.player().getFoodData().getFoodLevel() == 4 && lunch.player().getInventory().countItem(Items.BOWL) == 1,
                "The remaining portions keep their components, with normal hunger gain and one bowl returned");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void failingLunchFoodQueryKeepsServingAndAllowsRetry(GameTestHelper h) {
        var lunch = lunch(h);
        boolean failed = false;
        try { feed(lunch, 1, (stack, player) -> { throw new IllegalStateException("Expected food query failure"); }); }
        catch (IllegalStateException expected) { failed = expected.getMessage().equals("Expected food query failure"); }
        h.assertTrue(failed && portions(lunch.box()) == 3, "A failing preflight query has not spent a portion");
        h.assertTrue(LunchBoxItem.feedOne(lunch.player()) && portions(lunch.box()) == 2, "The feeding guard clears so a later query can retry");
        h.succeed();
    }

    private static void menuQuery(GameTestHelper h, int kind) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(lunch.food().copyWithCount(2))));
        lunch.player().getInventory().setItem(9, lunch.food().copy());
        var menu = new LunchBoxMenu(1, lunch.player().getInventory(), 0);
        var replacement = box(new ItemStack(Items.APPLE, 7));
        var key = lunch.food().get(DataComponents.CUSTOM_NAME);
        PROBES.put(key, new Probe(1, (stack, player) -> {
            if (kind == 0) lunch.player().getInventory().setItem(0, replacement);
            else if (kind == 1) menu.removed(lunch.player());
            else lunch.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
        }));
        try {
            h.assertTrue(menu.quickMoveStack(lunch.player(), 9).isEmpty(), "A changed menu or source cancels the old food deposit");
            var source = lunch.player().getInventory().getItem(9);
            h.assertTrue(kind == 2 ? source.is(Items.DIAMOND) && source.getCount() == 7 : ItemStack.matches(source, lunch.food()),
                    "The source stack or callback replacement remains in the player's inventory");
            h.assertTrue(portions(lunch.box()) == 2 && portions(replacement) == 7, "Neither box gains or loses food from the cancelled deposit");
        } finally { PROBES.remove(key); }
        h.succeed();
    }

    @GameTest(template = "empty") public static void lunchDepositCannotLoseFoodWhenBoxIsReplaced(GameTestHelper h) { menuQuery(h, 0); }
    @GameTest(template = "empty") public static void lunchDepositStopsWhenFoodQueryClosesMenu(GameTestHelper h) { menuQuery(h, 1); }
    @GameTest(template = "empty") public static void lunchDepositPreservesSourceReplacement(GameTestHelper h) { menuQuery(h, 2); }

    @GameTest(template = "empty")
    public static void lunchFoodQueryCannotRecursivelyDeposit(GameTestHelper h) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(lunch.food().copyWithCount(2))));
        lunch.player().getInventory().setItem(9, lunch.food().copy());
        var menu = new LunchBoxMenu(1, lunch.player().getInventory(), 0);
        var key = lunch.food().get(DataComponents.CUSTOM_NAME);
        boolean[] nested = {false};
        PROBES.put(key, new Probe(1, (stack, player) -> nested[0] = !menu.quickMoveStack(lunch.player(), 9).isEmpty()));
        try {
            h.assertTrue(!menu.quickMoveStack(lunch.player(), 9).isEmpty() && !nested[0]
                            && portions(lunch.box()) == 5 && lunch.player().getInventory().getItem(9).isEmpty(),
                    "The original deposit completes once and a nested query cannot start another transfer");
        } finally { PROBES.remove(key); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directLunchInsertionRechecksClosedMenuAfterFoodQuery(GameTestHelper h) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        var menu = new LunchBoxMenu(1, lunch.player().getInventory(), 0);
        var source = lunch.food().copy();
        var key = lunch.food().get(DataComponents.CUSTOM_NAME);
        PROBES.put(key, new Probe(1, (stack, player) -> menu.removed(lunch.player())));
        try {
            menu.getSlot(0).safeInsert(source);
            h.assertTrue(ItemStack.matches(source, lunch.food()) && portions(lunch.box()) == 0 && menu.getSlot(0).getItem().isEmpty(),
                    "A food query that closes the menu cannot consume the insertion stack into an old inventory");
        } finally { PROBES.remove(key); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryLunchDepositsKeepNamedFoodAndSettings(GameTestHelper h) {
        var lunch = lunch(h);
        lunch.box().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        lunch.player().getInventory().setItem(9, lunch.food().copy());
        LunchBoxItem.toggle(lunch.box());
        LunchBoxItem.cyclePriority(lunch.box(), 0);
        var menu = new LunchBoxMenu(1, lunch.player().getInventory(), 0);
        h.assertTrue(!menu.quickMoveStack(lunch.player(), 9).isEmpty(), "An ordinary modded-food deposit succeeds");
        menu.clickMenuButton(lunch.player(), 1);
        var food = lunch.box().get(DataComponents.CONTAINER).nonEmptyStream().findFirst().orElseThrow();
        h.assertTrue(ItemStack.matches(food, lunch.food()) && !LunchBoxItem.enabled(lunch.box()) && LunchBoxItem.priority(lunch.box(), 0) == 2,
                "Depositing and sorting preserve named food, priority and the paused setting");
        h.succeed();
    }
}
