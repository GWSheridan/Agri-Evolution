package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("agri_evolution")
@PrefixGameTestTemplate(false)
public final class DeliveryAndStorageGameTests {
    private static int tart() {
        for (int i = 0; i < CookingFoods.ALL.size(); i++) if (CookingFoods.ALL.get(i).id().equals("strawberry_tart")) return i;
        throw new IllegalStateException("Missing tart");
    }

    private static BatchCookingBlockEntity kitchen(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, AEBlocks.BATCH_COOKING_STATION.get().defaultBlockState(), 3);
        return (BatchCookingBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void withTartRecipe(GameTestHelper h, List<Ingredient> ingredients, Runnable test) {
        var manager = h.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var changed = new ArrayList<>(original);
        var id = ResourceLocation.fromNamespaceAndPath("agri_evolution", "strawberry_tart");
        changed.removeIf(recipe -> recipe.id().equals(id));
        var entries = NonNullList.<Ingredient>create();
        entries.addAll(ingredients);
        changed.add(new RecipeHolder<>(id, new ShapelessRecipe("", CraftingBookCategory.MISC,
                new ItemStack(CookingFoods.ALL.get(tart()).item().get()), entries)));
        manager.replaceRecipes(changed);
        try { test.run(); } finally { manager.replaceRecipes(original); }
    }

    private static void fillInventory(Player player) {
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
    }

    private static VerticalGreenhouseBlockEntity column(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var item = new ItemStack(AEItems.VERTICAL_GREENHOUSE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
        var greenhouse = (VerticalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        h.assertTrue(greenhouse != null && greenhouse.complete(), "The fixture is a complete column");
        return greenhouse;
    }

    @GameTest(template = "empty")
    public static void cookingFallbackKeepsMandatoryContainers(GameTestHelper h) {
        withTartRecipe(h, List.of(Ingredient.of(AEItems.STRAWBERRY.get(), AEItems.BERRY_JAM.get()), Ingredient.of(Items.MILK_BUCKET)), () -> {
            for (int example = 0; example < 2; example++) {
                var pos = h.absolutePos(new BlockPos(4, 210 + example * 4, 4));
                var kitchen = kitchen(h, pos);
                kitchen.enqueue(tart(), 1);
                kitchen.setItem(0, new ItemStack(AEItems.BERRY_JAM.get()));
                kitchen.setItem(1, new ItemStack(AEItems.STRAWBERRY.get()));
                kitchen.setItem(2, new ItemStack(Items.MILK_BUCKET));
                for (int slot = BatchCookingBlockEntity.RETURNS; slot < BatchCookingBlockEntity.SLOTS; slot++)
                    kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
                kitchen.setItem(BatchCookingBlockEntity.RETURNS, example == 0 ? ItemStack.EMPTY : new ItemStack(Items.BUCKET, 15));
                kitchen.cookOne(h.getLevel());
                h.assertTrue(kitchen.order(0) == null && kitchen.getItem(BatchCookingBlockEntity.FOOD).is(CookingFoods.ALL.get(tart()).item().get()),
                        "Fresh fruit can be used while the required milk bucket still returns its container");
                h.assertTrue(kitchen.getItem(0).is(AEItems.BERRY_JAM.get()) && kitchen.getItem(1).isEmpty() && kitchen.getItem(2).isEmpty()
                        && kitchen.getItem(BatchCookingBlockEntity.RETURNS).is(Items.BUCKET)
                        && kitchen.getItem(BatchCookingBlockEntity.RETURNS).getCount() == (example == 0 ? 1 : 16), "Only chosen ingredients and the required bucket are committed");
            }
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cookingFallbackReassignsOverlappingFreshIngredients(GameTestHelper h) {
        withTartRecipe(h, List.of(Ingredient.of(AEItems.STRAWBERRY.get(), AEItems.BERRY_JAM.get()),
                Ingredient.of(AEItems.STRAWBERRY.get(), AEItems.TOMATO.get()), Ingredient.of(Items.MILK_BUCKET)), () -> {
            var kitchen = kitchen(h, h.absolutePos(new BlockPos(4, 220, 4)));
            kitchen.enqueue(tart(), 1);
            kitchen.setItem(0, new ItemStack(AEItems.STRAWBERRY.get()));
            kitchen.setItem(1, new ItemStack(AEItems.TOMATO.get()));
            kitchen.setItem(2, new ItemStack(AEItems.BERRY_JAM.get()));
            kitchen.setItem(3, new ItemStack(Items.MILK_BUCKET));
            for (int slot = BatchCookingBlockEntity.RETURNS + 1; slot < BatchCookingBlockEntity.SLOTS; slot++) kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
            kitchen.cookOne(h.getLevel());
            h.assertTrue(kitchen.order(0) == null && kitchen.getItem(0).isEmpty() && kitchen.getItem(1).isEmpty()
                    && kitchen.getItem(2).is(AEItems.BERRY_JAM.get()) && kitchen.getItem(3).isEmpty(),
                    "Overlapping alternatives are reassigned to the two fresh stacks instead of an unnecessary preserve");
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cookingWaitsWithoutChargingWhenRequiredReturnsCannotFit(GameTestHelper h) {
        withTartRecipe(h, List.of(Ingredient.of(AEItems.STRAWBERRY.get()), Ingredient.of(Items.MILK_BUCKET)), () -> {
            var kitchen = kitchen(h, h.absolutePos(new BlockPos(4, 220, 4)));
            kitchen.enqueue(tart(), 1);
            kitchen.setItem(0, new ItemStack(AEItems.STRAWBERRY.get()));
            kitchen.setItem(1, new ItemStack(Items.MILK_BUCKET));
            for (int slot = BatchCookingBlockEntity.RETURNS; slot < BatchCookingBlockEntity.SLOTS; slot++) kitchen.setItem(slot, new ItemStack(Items.DIRT, 64));
            kitchen.cookOne(h.getLevel());
            h.assertTrue(kitchen.status() == BatchCookingBlockEntity.Status.CONTAINERS_FULL && kitchen.order(0).count() == 1
                    && kitchen.getItem(0).getCount() == 1 && kitchen.getItem(1).is(Items.MILK_BUCKET)
                    && kitchen.getItem(BatchCookingBlockEntity.FOOD).isEmpty(), "An unavoidable full return row preserves all ingredients and the queued meal");
            kitchen.setItem(BatchCookingBlockEntity.RETURNS, ItemStack.EMPTY);
            kitchen.cookOne(h.getLevel());
            h.assertTrue(kitchen.order(0) == null && kitchen.getItem(BatchCookingBlockEntity.RETURNS).is(Items.BUCKET), "Freeing one return slot resumes the same batch");
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullCreativeInventoryReceivesOrderAndPreserveRewards(GameTestHelper h) {
        for (boolean preserving : new boolean[]{false, true}) {
            var pos = h.absolutePos(new BlockPos(4, preserving ? 225 : 215, 4));
            var block = preserving ? AEBlocks.PRESERVING_STATION.get() : AEBlocks.GARDEN_ORDER_BOARD.get();
            h.getLevel().setBlock(pos, block.defaultBlockState(), 3);
            var player = h.makeMockPlayer(GameType.CREATIVE);
            player.getAbilities().instabuild = true;
            player.setPos(Vec3.atCenterOf(pos));
            fillInventory(player);
            var menu = new GardenWorkMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos), preserving);
            var job = menu.job(0);
            for (int i = 0; i < job.ingredients().size(); i++) {
                var ingredient = job.ingredients().get(i);
                player.getInventory().setItem(i, new ItemStack(ingredient.item().get(), ingredient.count() + 1));
            }
            h.assertTrue(menu.clickMenuButton(player, menu.action(0)), "The delivery completes with partial ingredient stacks");
            int rewards = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                    .map(ItemEntity::getItem).filter(stack -> stack.is(job.reward().item().get())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(rewards == job.reward().count(), "A full Creative inventory drops the exact reward instead of discarding it");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void seedCommissionIsClaimedBeforeRewardCallbacks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        var seeds = new ItemStack(AEItems.TOMATO_SEED.get(), 8);
        GenomeStorage.putOnSeed(seeds, new CropGenome(80, 80, 80));
        player.getInventory().setItem(0, seeds);
        var menu = new GardenWorkMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos), false);
        menu.clickMenuButton(player, menu.pageAction(GardenWorkMenu.SEED_PAGE));
        boolean[] observed = {false}, secondClaim = {false}, saved = {false};
        java.util.function.Consumer<EntityJoinLevelEvent> listener = event -> {
            if (observed[0] || !(event.getEntity() instanceof ItemEntity item) || !item.getItem().is(AEItems.NUTRIENT_COMPOST.get())) return;
            observed[0] = true;
            saved[0] = menu.claimed(0);
            secondClaim[0] = menu.clickMenuButton(player, menu.action(0));
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(listener);
        try { h.assertTrue(menu.clickMenuButton(player, menu.action(0)), "The first commission delivery succeeds"); }
        finally { bus.unregister(listener); }
        h.assertTrue(observed[0] && saved[0] && !secondClaim[0] && seeds.getCount() == 4 && menu.claimed(0),
                "A reward callback cannot collect the same one-time commission twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void orderProgressIsSavedBeforeRewardCallbacks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.GARDEN_ORDER_BOARD.get().defaultBlockState(), 3);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        fillInventory(player);
        var menu = new GardenWorkMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos), false);
        for (int i = 0; i < menu.job(0).ingredients().size(); i++) {
            var ingredient = menu.job(0).ingredients().get(i);
            player.getInventory().setItem(i, new ItemStack(ingredient.item().get(), ingredient.count() + 1));
        }
        boolean[] saved = {false};
        java.util.function.Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getEntity() instanceof ItemEntity item && item.getItem().is(Items.EMERALD)) saved[0] = GardenWork.completed(player, 0) == 1;
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(listener);
        try { h.assertTrue(menu.clickMenuButton(player, menu.action(0)), "A regular delivery completes"); }
        finally { bus.unregister(listener); }
        h.assertTrue(saved[0] && GardenWork.completed(player, 0) == 1, "Item-delivery callbacks see the new market progression immediately");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedCookingStationRejectsLateInventoryClicks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var kitchen = kitchen(h, pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var menu = new BatchCookingMenu(1, player.getInventory(), kitchen);
        kitchen.setItem(BatchCookingBlockEntity.FOOD, new ItemStack(Items.DIAMOND, 2));
        kitchen.setRemoved();
        h.assertFalse(menu.stillValid(player), "Removed cooking stations invalidate their menus");
        menu.clicked(BatchCookingBlockEntity.FOOD, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, BatchCookingBlockEntity.FOOD).isEmpty()
                && kitchen.getItem(BatchCookingBlockEntity.FOOD).getCount() == 2, "Late pickup and shift-click cannot take detached cooking output");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedGreenhouseStorageRejectsLateInventoryClicks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        h.getLevel().setBlock(pos, AEBlocks.BOTANICAL_GREENHOUSE.get().defaultBlockState(), 3);
        var greenhouse = (BotanicalGreenhouseBlockEntity) h.getLevel().getBlockEntity(pos);
        greenhouse.setItem(0, new ItemStack(Items.DIAMOND, 2));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var menu = new BotanicalGreenhouseStorageMenu(1, player.getInventory(), greenhouse);
        greenhouse.setRemoved();
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, 0).isEmpty()
                && greenhouse.getItem(0).getCount() == 2, "Detached greenhouse storage rejects both late transfer paths");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseRejectsLateInventoryClicks(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(4, 220, 4));
        var greenhouse = column(h, pos);
        greenhouse.setItem(VerticalGreenhouseBlockEntity.HARVEST, new ItemStack(Items.DIAMOND, 2));
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos));
        var menu = new VerticalGreenhouseMenu(1, player.getInventory(), greenhouse);
        greenhouse.setRemoved();
        h.assertFalse(menu.stillValid(player), "Removed columns invalidate the old menu even while their block state remains");
        menu.clicked(VerticalGreenhouseBlockEntity.HARVEST, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.getCarried().isEmpty() && menu.quickMoveStack(player, VerticalGreenhouseBlockEntity.HARVEST).isEmpty()
                && greenhouse.getItem(VerticalGreenhouseBlockEntity.HARVEST).getCount() == 2, "Late vertical greenhouse clicks preserve detached output");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void removedVerticalGreenhouseRejectsSidedItemTransfers(GameTestHelper h) {
        var greenhouse = column(h, h.absolutePos(new BlockPos(4, 220, 4)));
        var seed = new ItemStack(AEItems.TOMATO_SEED.get());
        var result = new ItemStack(Items.DIAMOND);
        h.assertTrue(greenhouse.canPlaceItem(0, seed)
                && greenhouse.canTakeItemThroughFace(VerticalGreenhouseBlockEntity.HARVEST, result, Direction.DOWN), "Current column item access is available");
        greenhouse.setRemoved();
        h.assertFalse(greenhouse.canPlaceItem(0, seed)
                || greenhouse.canTakeItemThroughFace(VerticalGreenhouseBlockEntity.HARVEST, result, Direction.DOWN), "Stale automation access cannot insert or extract after removal");
        h.succeed();
    }
}
