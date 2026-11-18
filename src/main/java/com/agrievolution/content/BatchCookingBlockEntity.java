package com.agrievolution.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BatchCookingBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int FOOD = 18, RETURNS = FOOD + 6, SLOTS = RETURNS + 3, QUEUE_LIMIT = 8;
    private static final int[] INPUTS = IntStream.range(0, FOOD).toArray();
    private static final int[] OUTPUTS = IntStream.range(FOOD, SLOTS).toArray();
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ArrayList<Order> queue = new ArrayList<>();
    private Status status = Status.IDLE;
    record Order(int dish, int count) {}
    public enum Status { IDLE, COOKING, MISSING_INGREDIENTS, OUTPUT_FULL, RECIPE_UNAVAILABLE }

    public BatchCookingBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.BATCH_COOKING_STATION.get(), pos, state);
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.batch_cooking_station"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new BatchCookingMenu(id, inventory, this); }
    @Override public int getContainerSize() { return SLOTS; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < FOOD; }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUTS : INPUTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && slot < FOOD; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side == Direction.DOWN && slot >= FOOD; }

    Order order(int index) { return index < queue.size() ? queue.get(index) : null; }
    Status status() { return status; }

    boolean enqueue(int dish, int count) {
        if (queue.size() >= QUEUE_LIMIT || dish < 0 || dish >= CookingFoods.ALL.size() || count < 1 || count > 16) return false;
        queue.add(new Order(dish, count));
        setChanged();
        return true;
    }

    boolean cancel(int index, int dish, int count) {
        var job = order(index);
        if (job == null || job.dish() != dish || job.count() != count) return false;
        queue.remove(index);
        setChanged();
        return true;
    }

    void serverTick() {
        if (level instanceof ServerLevel server && server.getGameTime() % 20 == 0) cookOne(server);
    }

    void cookOne(ServerLevel server) {
        var job = order(0);
        if (job == null) { status = Status.IDLE; return; }
        var dish = CookingFoods.ALL.get(job.dish());
        var recipe = server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution", dish.id())).orElse(null);
        if (recipe == null || !(recipe.value() instanceof ShapelessRecipe shaped)
                || !shaped.getResultItem(server.registryAccess()).is(dish.item().get())) {
            status = Status.RECIPE_UNAVAILABLE;
            return;
        }
        var ingredients = shaped.getIngredients();
        if (ingredients.isEmpty() || ingredients.size() > 9) { status = Status.RECIPE_UNAVAILABLE; return; }
        int[] matches = new int[ingredients.size()];
        Arrays.fill(matches, -1);
        for (int i = 0; i < matches.length; i++) {
            if (!findSlot(ingredients, i, matches, new boolean[FOOD])) { status = Status.MISSING_INGREDIENTS; return; }
        }
        var grid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < matches.length; i++) grid.set(i, items.get(matches[i]).copyWithCount(1));
        var input = CraftingInput.of(3, 3, grid);
        if (!shaped.matches(input, server)) { status = Status.RECIPE_UNAVAILABLE; return; }

        var result = shaped.assemble(input, server.registryAccess());
        var next = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) next.set(i, items.get(i).copy());
        if (result.isEmpty() || !result.is(dish.item().get())) { status = Status.RECIPE_UNAVAILABLE; return; }
        if (!insert(next, result, FOOD, RETURNS)) { status = Status.OUTPUT_FULL; return; }
        for (ItemStack remainder : shaped.getRemainingItems(input)) {
            if (!remainder.isEmpty() && !insert(next, remainder, RETURNS, SLOTS)) {
                status = Status.OUTPUT_FULL;
                return;
            }
        }
        for (int slot : matches) {
            var stack = next.get(slot);
            stack.shrink(1);
            if (stack.isEmpty()) next.set(slot, ItemStack.EMPTY);
        }
        items = next;
        if (job.count() == 1) queue.removeFirst();
        else queue.set(0, new Order(job.dish(), job.count() - 1));
        status = Status.COOKING;
        setChanged();
    }

    /** Reassign earlier flexible ingredients when a later one needs their stack. */
    private boolean findSlot(List<Ingredient> ingredients, int ingredient, int[] matches, boolean[] visited) {
        for (int slot = 0; slot < FOOD; slot++) {
            var stack = items.get(slot);
            if (visited[slot] || stack.isEmpty() || !ingredients.get(ingredient).test(stack)) continue;
            visited[slot] = true;
            int used = 0;
            for (int taken : matches) if (taken == slot) used++;
            if (used < stack.getCount()) {
                matches[ingredient] = slot;
                return true;
            }
            for (int i = 0; i < matches.length; i++) {
                if (matches[i] != slot || !findSlot(ingredients, i, matches, visited)) continue;
                matches[ingredient] = slot;
                return true;
            }
        }
        return false;
    }

    private static boolean insert(NonNullList<ItemStack> into, ItemStack addition, int first, int end) {
        int remaining = addition.getCount();
        for (int slot = first; slot < end && remaining > 0; slot++) {
            var existing = into.get(slot);
            if (!ItemStack.isSameItemSameComponents(existing, addition)) continue;
            int count = Math.min(remaining, existing.getMaxStackSize() - existing.getCount());
            existing.grow(count);
            remaining -= count;
        }
        for (int slot = first; slot < end && remaining > 0; slot++) {
            if (!into.get(slot).isEmpty()) continue;
            int count = Math.min(remaining, addition.getMaxStackSize());
            into.set(slot, addition.copyWithCount(count));
            remaining -= count;
        }
        return remaining == 0;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        var entries = new ListTag();
        for (var order : queue) {
            var entry = new CompoundTag();
            entry.putString("Dish", CookingFoods.ALL.get(order.dish()).id());
            entry.putInt("Count", order.count());
            entries.add(entry);
        }
        tag.put("CookingQueue", entries);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        queue.clear();
        for (Tag entry : tag.getList("CookingQueue", Tag.TAG_COMPOUND)) {
            if (queue.size() >= QUEUE_LIMIT) break;
            var record = (CompoundTag) entry;
            String dish = record.getString("Dish");
            int count = record.getInt("Count");
            if (count < 1 || count > 16) continue;
            for (int i = 0; i < CookingFoods.ALL.size(); i++) {
                if (!CookingFoods.ALL.get(i).id().equals(dish)) continue;
                queue.add(new Order(i, count));
                break;
            }
        }
    }
}
