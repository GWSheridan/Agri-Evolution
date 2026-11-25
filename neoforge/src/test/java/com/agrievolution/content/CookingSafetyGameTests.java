package com.agrievolution.content;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class CookingSafetyGameTests {
    private record Kitchen(BlockPos pos, BatchCookingBlockEntity entity, Player player, BatchCookingMenu menu) {}

    private static int tart() {
        for (int i = 0; i < CookingFoods.ALL.size(); i++) if (CookingFoods.ALL.get(i).id().equals("strawberry_tart")) return i;
        throw new IllegalStateException("Missing tart");
    }

    private static Kitchen kitchen(GameTestHelper h, GameType mode) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.BATCH_COOKING_STATION.get().defaultBlockState(), 2);
        var entity = (BatchCookingBlockEntity) h.getLevel().getBlockEntity(pos);
        entity.setItem(0, new ItemStack(Items.WHEAT, 2));
        h.assertTrue(entity.enqueue(tart(), 2), "Queue two servings before the check");
        var player = h.makeMockPlayer(mode);
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        return new Kitchen(pos, entity, player, new BatchCookingMenu(1, player.getInventory(), entity));
    }

    private static final class HookRecipe extends ShapelessRecipe {
        Runnable assembling, remainders, restore;
        HookRecipe() {
            super("", CraftingBookCategory.MISC, new ItemStack(CookingFoods.ALL.get(tart()).item().get()),
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.WHEAT)));
        }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
            var callback = assembling;
            assembling = null;
            if (callback != null) callback.run();
            return super.assemble(input, registries);
        }
        @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            var callback = remainders;
            remainders = null;
            if (callback != null) callback.run();
            return super.getRemainingItems(input);
        }
    }

    private static void withRecipe(GameTestHelper h, Consumer<HookRecipe> test) {
        var manager = h.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var changed = new ArrayList<>(original);
        var id = ResourceLocation.fromNamespaceAndPath("agri_evolution", "strawberry_tart");
        changed.removeIf(recipe -> recipe.id().equals(id));
        var recipe = new HookRecipe();
        recipe.restore = () -> manager.replaceRecipes(original);
        changed.add(new RecipeHolder<>(id, recipe));
        manager.replaceRecipes(changed);
        try { test.accept(recipe); } finally { recipe.restore.run(); }
    }

    private static void untouched(GameTestHelper h, Kitchen kitchen) {
        h.assertTrue(kitchen.entity().getItem(0).is(Items.WHEAT) && kitchen.entity().getItem(0).getCount() == 2
                        && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).isEmpty()
                        && kitchen.entity().order(0) != null && kitchen.entity().order(0).count() == 2,
                "Rejected cooking keeps the ingredients, meal output and queued count");
    }

    @GameTest(template = "empty")
    public static void removedKitchenCannotCookThroughOldReference(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.entity().setRemoved();
        withRecipe(h, recipe -> kitchen.entity().cookOne(h.getLevel()));
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cookingRejectsAnotherDimension(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        var other = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(other != null && other != h.getLevel(), "The fixture has another dimension");
        withRecipe(h, recipe -> kitchen.entity().cookOne(other));
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeCallbackCannotCookSameKitchenRecursively(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.assembling = () -> kitchen.entity().cookOne(h.getLevel());
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().getItem(0).getCount() == 1 && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).getCount() == 1
                        && kitchen.entity().order(0).count() == 1, "A recursive callback cannot prepare a second serving or undercharge the queue");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cancelledCookingHeadCannotRemoveNextBatch(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        int cancelled = kitchen.entity().order(0).id();
        kitchen.entity().enqueue(tart(), 4);
        int next = kitchen.entity().order(1).id();
        withRecipe(h, recipe -> {
            recipe.remainders = () -> kitchen.entity().cancel(cancelled);
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().order(0) != null && kitchen.entity().order(0).id() == next
                        && kitchen.entity().order(0).count() == 4 && kitchen.entity().order(1) == null
                        && kitchen.entity().getItem(0).getCount() == 2 && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).isEmpty(),
                "Cancelling during a recipe callback does not cook the cancelled dish or decrement the next order");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeCallbackCannotOverwriteNewMealSlotContents(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.remainders = () -> kitchen.entity().setItem(BatchCookingBlockEntity.FOOD, new ItemStack(Items.DIAMOND, 5));
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).is(Items.DIAMOND)
                        && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).getCount() == 5
                        && kitchen.entity().getItem(0).getCount() == 2 && kitchen.entity().order(0).count() == 2,
                "Items inserted during remainder calculation are preserved without cooking the old ingredients");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeCallbackCannotChargeReplacementIngredient(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.assembling = () -> kitchen.entity().setItem(0, new ItemStack(Items.DIAMOND, 5));
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().getItem(0).is(Items.DIAMOND) && kitchen.entity().getItem(0).getCount() == 5
                        && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).isEmpty() && kitchen.entity().order(0).count() == 2,
                "Replacing a matched ingredient during assembly cancels the old attempt without charging the replacement");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changedIngredientComponentsCancelCookingAttempt(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.remainders = () -> kitchen.entity().getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("New wheat lot"));
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().getItem(0).getCount() == 2
                        && Component.literal("New wheat lot").equals(kitchen.entity().getItem(0).get(DataComponents.CUSTOM_NAME))
                        && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).isEmpty() && kitchen.entity().order(0).count() == 2,
                "A component change is preserved and invalidates the original ingredient snapshot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void brokenKitchenCannotRestoreItsDroppedInventory(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.remainders = () -> h.getLevel().removeBlock(kitchen.pos(), false);
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().isRemoved() && kitchen.entity().isEmpty(),
                "Breaking during a recipe callback cannot restore ingredients or a meal into the detached machine");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeReloadCancelsTheOldCookingAttempt(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.remainders = recipe.restore;
            kitchen.entity().cookOne(h.getLevel());
        });
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeCallbackMayAppendAnotherCookingBatch(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.remainders = () -> kitchen.entity().enqueue(tart(), 4);
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().order(0).count() == 1 && kitchen.entity().order(1).count() == 4
                        && kitchen.entity().getItem(0).getCount() == 1 && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).getCount() == 1,
                "Appending a later batch does not invalidate or overwrite an unchanged current order");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void failedRecipeCallbackDoesNotBlockLaterCooking(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        withRecipe(h, recipe -> {
            recipe.assembling = () -> { throw new IllegalStateException("Expected test callback failure"); };
            boolean failed = false;
            try { kitchen.entity().cookOne(h.getLevel()); }
            catch (IllegalStateException expected) { failed = expected.getMessage().equals("Expected test callback failure"); }
            h.assertTrue(failed, "The callback failure reaches its caller");
            untouched(h, kitchen);
            kitchen.entity().cookOne(h.getLevel());
        });
        h.assertTrue(kitchen.entity().order(0).count() == 1 && kitchen.entity().getItem(BatchCookingBlockEntity.FOOD).getCount() == 1,
                "The kitchen can retry normally after an exceptional callback");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedKitchenScreenCannotPickUpIngredients(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.menu().removed(kitchen.player());
        kitchen.menu().clicked(0, 0, ClickType.PICKUP, kitchen.player());
        h.assertTrue(kitchen.menu().getCarried().isEmpty(), "A late click cannot pick up ingredients after closing");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedKitchenScreenCannotChangeQueue(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        int cancel = kitchen.menu().cancelAction(0);
        kitchen.menu().removed(kitchen.player());
        h.assertTrue(!kitchen.menu().clickMenuButton(kitchen.player(), BatchCookingMenu.SELECT_NEXT)
                        && !kitchen.menu().clickMenuButton(kitchen.player(), cancel)
                        && !kitchen.menu().clickMenuButton(kitchen.player(), BatchCookingMenu.queueAction(tart(), 0)),
                "Closed screens cannot browse, cancel or enqueue another batch");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void foreignViewerCannotUseKitchenControls(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        var other = h.makeMockPlayer(GameType.SURVIVAL);
        other.setPos(net.minecraft.world.phys.Vec3.atCenterOf(kitchen.pos()));
        h.assertFalse(kitchen.menu().clickMenuButton(other, BatchCookingMenu.queueAction(tart(), 0)),
                "Another nearby player cannot act through this viewer's screen");
        h.assertTrue(kitchen.entity().order(1) == null, "No foreign batch was queued");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spectatorCannotWithdrawCookingIngredients(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SPECTATOR);
        kitchen.menu().clicked(0, 0, ClickType.PICKUP, kitchen.player());
        h.assertTrue(kitchen.menu().getCarried().isEmpty() && kitchen.menu().quickMoveStack(kitchen.player(), 0).isEmpty(),
                "Spectators cannot take ingredients with pickup or shift-click");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deadViewerCannotWithdrawCookingIngredients(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.player().setHealth(0);
        h.assertTrue(kitchen.menu().quickMoveStack(kitchen.player(), 0).isEmpty(), "Dead viewers cannot use the old inventory");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedKitchenSlotCannotBeTakenDirectly(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.entity().setRemoved();
        h.assertTrue(kitchen.menu().getSlot(0).safeTake(1, 1, kitchen.player()).isEmpty(), "Retained slots cannot withdraw from a removed kitchen");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void closedKitchenSlotsRejectDirectTransfers(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        kitchen.menu().removed(kitchen.player());
        var offered = new ItemStack(Items.WHEAT, 3);
        kitchen.menu().getSlot(0).safeInsert(offered);
        h.assertTrue(offered.getCount() == 3 && kitchen.menu().getSlot(BatchCookingBlockEntity.SLOTS + 27)
                        .safeTake(1, 1, kitchen.player()).isEmpty(), "Machine and player slots both enforce screen access");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedKitchenRejectsQueueChanges(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        int id = kitchen.entity().order(0).id();
        kitchen.entity().setRemoved();
        h.assertTrue(!kitchen.entity().enqueue(tart(), 1) && !kitchen.entity().cancel(id),
                "Removed kitchens ignore direct enqueue and cancellation calls");
        untouched(h, kitchen);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedKitchenRejectsSidedAutomation(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        kitchen.entity().setRemoved();
        h.assertTrue(kitchen.entity().getSlotsForFace(Direction.UP).length == 0
                        && kitchen.entity().getSlotsForFace(Direction.DOWN).length == 0
                        && !kitchen.entity().canPlaceItemThroughFace(0, new ItemStack(Items.WHEAT), Direction.UP)
                        && !kitchen.entity().canTakeItemThroughFace(BatchCookingBlockEntity.FOOD, ItemStack.EMPTY, Direction.DOWN),
                "Detached automation references expose no input or output slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidKitchenIndicesAreRejected(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.SURVIVAL);
        h.assertTrue(kitchen.entity().order(-1) == null && kitchen.entity().order(Integer.MAX_VALUE) == null
                        && !kitchen.entity().canPlaceItem(-1, new ItemStack(Items.WHEAT))
                        && !kitchen.entity().canPlaceItemThroughFace(-1, new ItemStack(Items.WHEAT), Direction.UP)
                        && !kitchen.entity().canTakeItemThroughFace(BatchCookingBlockEntity.SLOTS, ItemStack.EMPTY, Direction.DOWN),
                "Invalid queue and inventory indices neither throw nor advertise transferable slots");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void adventureKitchenInventoryAndQueueStillWork(GameTestHelper h) {
        var kitchen = kitchen(h, GameType.ADVENTURE);
        kitchen.player().getAbilities().mayBuild = false;
        kitchen.player().getInventory().setItem(0, new ItemStack(Items.WHEAT, 3));
        h.assertTrue(!kitchen.menu().quickMoveStack(kitchen.player(), BatchCookingBlockEntity.SLOTS + 27).isEmpty()
                        && kitchen.entity().getItem(0).getCount() == 5
                        && kitchen.menu().clickMenuButton(kitchen.player(), BatchCookingMenu.queueAction(tart(), 0)),
                "Ordinary inventory and cooking use do not require build permission");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void kitchenPreviewWorksWithoutPlayer(GameTestHelper h) {
        var menu = new BatchCookingMenu(1, new Inventory(null));
        h.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.WHEAT))
                        && !menu.getSlot(BatchCookingBlockEntity.FOOD).mayPlace(new ItemStack(Items.WHEAT)),
                "World-free previews retain input and result slot rules");
        h.succeed();
    }
}
