package com.agrievolution.content;

import com.agrievolution.platform.Game;

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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BatchCookingBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int FOOD = 18, RETURNS = FOOD + 6, SLOTS = RETURNS + 3, QUEUE_LIMIT = 8;
    private static final int[] INPUTS = IntStream.range(0, FOOD).toArray();
    private static final int[] OUTPUTS = IntStream.range(FOOD, SLOTS).toArray();
    private static final int[] NO_SLOTS = {};
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ArrayList<Order> queue = new ArrayList<>();
    private Status status = Status.IDLE;
    private int nextOrderId;
    private boolean cooking;
    record Order(int id, int dish, int count) {}
    private record PreparedMeal(Status status, NonNullList<ItemStack> contents) {}
    public enum Status { IDLE, COOKING, MISSING_INGREDIENTS, OUTPUT_FULL, RECIPE_UNAVAILABLE, CONTAINERS_FULL, QUEUED }

    public BatchCookingBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.BATCH_COOKING_STATION.get(), pos, state);
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.batch_cooking_station"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new BatchCookingMenu(id, inventory, this); }
    @Override public int getContainerSize() { return SLOTS; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot >= 0 && slot < FOOD && WorkstationAccess.isCurrent(this); }
    @Override public int[] getSlotsForFace(Direction side) { return !WorkstationAccess.isCurrent(this) ? NO_SLOTS : side == Direction.DOWN ? OUTPUTS : INPUTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot >= FOOD && slot < SLOTS && WorkstationAccess.isCurrent(this);
    }

    Order order(int index) { return index >= 0 && index < queue.size() ? queue.get(index) : null; }
    Status status() { return status; }

    boolean enqueue(int dish, int count) {
        if (!(level instanceof ServerLevel) || !WorkstationAccess.isCurrent(this)
                || queue.size() >= QUEUE_LIMIT || dish < 0 || dish >= CookingFoods.ALL.size() || count < 1 || count > 16) return false;
        addOrder(dish, count);
        if (queue.size() == 1) status = Status.QUEUED;
        setChanged();
        return true;
    }

    private void addOrder(int dish, int count) {
        do {
            nextOrderId = nextOrderId == Integer.MAX_VALUE ? 1 : nextOrderId + 1;
        } while (indexOfOrder(nextOrderId) >= 0);
        queue.add(new Order(nextOrderId, dish, count));
    }

    private int indexOfOrder(int id) {
        for (int row = 0; row < queue.size(); row++) {
            if (queue.get(row).id() == id) return row;
        }
        return -1;
    }

    boolean cancel(int id) {
        if (!(level instanceof ServerLevel) || !WorkstationAccess.isCurrent(this)) return false;
        int row = indexOfOrder(id);
        if (row < 0) return false;
        queue.remove(row);
        if (row == 0) status = queue.isEmpty() ? Status.IDLE : Status.QUEUED;
        setChanged();
        return true;
    }

    void serverTick() {
        if (level instanceof ServerLevel server && server.getGameTime() % 20 == 0) cookOne(server);
    }

    void cookOne(ServerLevel server) {
        if (cooking || level != server || !WorkstationAccess.isCurrent(this)) return;
        cooking = true;
        try {
            prepareCurrentOrder(server);
        } finally {
            cooking = false;
        }
    }

    private void prepareCurrentOrder(ServerLevel server) {
        var job = order(0);
        if (job == null) { status = Status.IDLE; return; }
        var before = copyItems(items);
        var dish = CookingFoods.ALL.get(job.dish());
        var shaped = recipe(server.getRecipeManager(), server.registryAccess(), dish);
        if (shaped == null) {
            if (sameAttempt(server, job, before)) status = Status.RECIPE_UNAVAILABLE;
            return;
        }
        var ingredients = shaped.getIngredients();
        int[] matches = matchIngredients(ingredients, before);
        if (matches == null) {
            if (sameAttempt(server, job, before)) status = Status.MISSING_INGREDIENTS;
            return;
        }
        var prepared = prepareMeal(server, shaped, dish, matches, before);
        if (prepared.status() == Status.CONTAINERS_FULL) {
            // Prefer fewer container returns without excluding required milk buckets or similar inputs.
            var fresh = matchFreshIngredients(ingredients, before);
            if (fresh != null) {
                var alternative = prepareMeal(server, shaped, dish, fresh, before);
                if (alternative.status() == Status.COOKING) prepared = alternative;
            }
        }
        var currentRecipe = server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution", dish.id())).orElse(null);
        if (!sameAttempt(server, job, before) || currentRecipe == null || currentRecipe.value() != shaped) return;
        status = prepared.status();
        if (status != Status.COOKING) return;
        // Commit inventory and the same queue head together, after all recipe callbacks have returned.
        items = prepared.contents();
        if (job.count() == 1) queue.removeFirst();
        else queue.set(0, new Order(job.id(), job.dish(), job.count() - 1));
        if (queue.isEmpty()) status = Status.IDLE;
        setChanged();
    }

    private static NonNullList<ItemStack> copyItems(List<ItemStack> source) {
        var copy = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) copy.set(i, source.get(i).copy());
        return copy;
    }

    private boolean sameAttempt(ServerLevel server, Order job, List<ItemStack> before) {
        if (level != server || !WorkstationAccess.isCurrent(this) || order(0) != job) return false;
        for (int i = 0; i < SLOTS; i++) if (!ItemStack.matches(items.get(i), before.get(i))) return false;
        return true;
    }

    private PreparedMeal prepareMeal(ServerLevel server, ShapelessRecipe recipe, CookingFoods.Dish dish, int[] matches,
                                     List<ItemStack> before) {
        var input = craftingInput(matches, before);
        if (!recipe.matches(input, server)) return new PreparedMeal(Status.RECIPE_UNAVAILABLE, null);
        var result = recipe.assemble(input, server.registryAccess());
        if (result.isEmpty() || !result.is(dish.item().get())) return new PreparedMeal(Status.RECIPE_UNAVAILABLE, null);
        var next = copyItems(before);
        if (!insert(next, result, FOOD, RETURNS)) return new PreparedMeal(Status.OUTPUT_FULL, null);
        if (!insertContainers(next, recipe, input)) return new PreparedMeal(Status.CONTAINERS_FULL, null);
        for (int slot : matches) {
            var stack = next.get(slot);
            stack.shrink(1);
            if (stack.isEmpty()) next.set(slot, ItemStack.EMPTY);
        }
        return new PreparedMeal(Status.COOKING, next);
    }

    public static ShapelessRecipe recipe(RecipeManager manager, HolderLookup.Provider registries, CookingFoods.Dish dish) {
        var entry = manager.byKey(ResourceLocation.fromNamespaceAndPath("agri_evolution", dish.id())).orElse(null);
        if (entry == null || !(entry.value() instanceof ShapelessRecipe recipe)
                || !recipe.getResultItem(registries).is(dish.item().get())
                || recipe.getIngredients().isEmpty() || recipe.getIngredients().size() > 9) return null;
        return recipe;
    }

    private int[] matchIngredients(List<Ingredient> ingredients, List<ItemStack> before) {
        int[] matches = new int[ingredients.size()];
        Arrays.fill(matches, -1);
        for (int i = 0; i < matches.length; i++) {
            if (!findSlot(ingredients, i, matches, new boolean[FOOD], before)) return null;
        }
        return matches;
    }

    private CraftingInput craftingInput(int[] matches, List<ItemStack> before) {
        var grid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < matches.length; i++) grid.set(i, before.get(matches[i]).copyWithCount(1));
        return CraftingInput.of(3, 3, grid);
    }

    private static boolean insertContainers(NonNullList<ItemStack> into, ShapelessRecipe recipe, CraftingInput input) {
        for (ItemStack remainder : recipe.getRemainingItems(input)) {
            if (!remainder.isEmpty() && !insert(into, remainder, RETURNS, SLOTS)) return false;
        }
        return true;
    }

    /** Reassign earlier flexible ingredients when a later one needs their stack. */
    private boolean findSlot(List<Ingredient> ingredients, int ingredient, int[] matches, boolean[] visited, List<ItemStack> before) {
        for (int slot = 0; slot < FOOD; slot++) {
            var stack = before.get(slot);
            if (visited[slot] || stack.isEmpty() || !ingredients.get(ingredient).test(stack)) continue;
            visited[slot] = true;
            int used = 0;
            for (int taken : matches) if (taken == slot) used++;
            if (used < stack.getCount()) {
                matches[ingredient] = slot;
                return true;
            }
            for (int i = 0; i < matches.length; i++) {
                if (matches[i] != slot || !findSlot(ingredients, i, matches, visited, before)) continue;
                matches[ingredient] = slot;
                return true;
            }
        }
        return false;
    }

    private static final class MatchEdge {
        final int to, reverse, cost;
        int capacity;
        MatchEdge(int to, int reverse, int capacity, int cost) {
            this.to = to;
            this.reverse = reverse;
            this.capacity = capacity;
            this.cost = cost;
        }
    }

    private static void addMatchEdge(List<List<MatchEdge>> graph, int from, int to, int capacity, int cost) {
        var forward = new MatchEdge(to, graph.get(to).size(), capacity, cost);
        var reverse = new MatchEdge(from, graph.get(from).size(), 0, -cost);
        graph.get(from).add(forward);
        graph.get(to).add(reverse);
    }

    /** Bounded minimum-cost matching: up to nine ingredients and eighteen input stacks. */
    private int[] matchFreshIngredients(List<Ingredient> ingredients, List<ItemStack> before) {
        int count = ingredients.size(), source = count + FOOD, sink = source + 1, nodes = sink + 1;
        var graph = new ArrayList<List<MatchEdge>>(nodes);
        for (int i = 0; i < nodes; i++) graph.add(new ArrayList<>());
        for (int i = 0; i < count; i++) {
            addMatchEdge(graph, source, i, 1, 0);
            for (int slot = 0; slot < FOOD; slot++) {
                var stack = before.get(slot);
                if (!stack.isEmpty() && ingredients.get(i).test(stack)) addMatchEdge(graph, i, count + slot, 1, 0);
            }
        }
        for (int slot = 0; slot < FOOD; slot++) {
            var stack = before.get(slot);
            int cost = !stack.isEmpty() && Game.hooks().hasCraftingRemainder(stack.copyWithCount(1)) ? 1 : 0;
            addMatchEdge(graph, count + slot, sink, Math.min(count, stack.getCount()), cost);
        }
        // Reverse edges let an earlier flexible ingredient move to another stack.
        // Choosing the cheapest augmenting path avoids an unnecessary preserve in that reassignment.
        for (int flow = 0; flow < count; flow++) {
            var distance = new int[nodes];
            Arrays.fill(distance, Integer.MAX_VALUE);
            distance[source] = 0;
            var previousNode = new int[nodes];
            var previousEdge = new MatchEdge[nodes];
            for (int pass = 0; pass < nodes - 1; pass++) {
                boolean changed = false;
                for (int from = 0; from < nodes; from++) {
                    if (distance[from] == Integer.MAX_VALUE) continue;
                    for (var edge : graph.get(from)) {
                        if (edge.capacity == 0 || distance[edge.to] <= distance[from] + edge.cost) continue;
                        distance[edge.to] = distance[from] + edge.cost;
                        previousNode[edge.to] = from;
                        previousEdge[edge.to] = edge;
                        changed = true;
                    }
                }
                if (!changed) break;
            }
            if (previousEdge[sink] == null) return null;
            for (int to = sink; to != source; to = previousNode[to]) {
                var edge = previousEdge[to];
                edge.capacity--;
                graph.get(to).get(edge.reverse).capacity++;
            }
        }
        var matches = new int[count];
        Arrays.fill(matches, -1);
        for (int i = 0; i < count; i++) {
            for (var edge : graph.get(i)) {
                if (edge.to >= count && edge.to < source && edge.capacity == 0) {
                    matches[i] = edge.to - count;
                    break;
                }
            }
            if (matches[i] < 0) return null;
        }
        return matches;
    }

    private static boolean insert(NonNullList<ItemStack> into, ItemStack addition, int first, int end) {
        int remaining = addition.getCount();
        for (int slot = first; slot < end && remaining > 0; slot++) {
            var existing = into.get(slot);
            if (!ItemStack.isSameItemSameComponents(existing, addition)) continue;
            int count = Math.min(remaining, Math.max(0, existing.getMaxStackSize() - existing.getCount()));
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
                addOrder(i, count);
                break;
            }
        }
        status = queue.isEmpty() ? Status.IDLE : Status.QUEUED;
    }
}
