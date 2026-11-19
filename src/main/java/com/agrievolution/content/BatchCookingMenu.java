package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BatchCookingMenu extends AbstractContainerMenu {
    public static final int SELECT_PREVIOUS = 0, SELECT_NEXT = 1;
    private static final int QUEUE_START = 2;
    private static final int[] BATCH_SIZES = {1, 4, 16};
    private static final int ORDER_DATA_START = 3, ORDER_DATA_SIZE = 4;
    private static final int DATA_COUNT = ORDER_DATA_START + BatchCookingBlockEntity.QUEUE_LIMIT * ORDER_DATA_SIZE;
    private final BatchCookingBlockEntity kitchen;
    private final ContainerData data;
    private int selection;

    public BatchCookingMenu(int id, Inventory inv) { this(id, inv, null); }

    BatchCookingMenu(int id, Inventory inv, BatchCookingBlockEntity kitchen) {
        super(AEMenus.BATCH_COOKING_STATION.get(), id);
        this.kitchen = kitchen;
        Container contents = kitchen == null ? new SimpleContainer(BatchCookingBlockEntity.SLOTS) : kitchen;
        data = kitchen == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return selection;
                if (index == 1) return kitchen.status().ordinal();
                if (index == 2) return recipeAvailable(selection) ? 1 : 0;
                int offset = index - ORDER_DATA_START;
                var order = kitchen.order(offset / ORDER_DATA_SIZE);
                if (order == null) return 0;
                // Vanilla menu data is signed 16-bit. Keep the full ID across two fields.
                return switch (offset % ORDER_DATA_SIZE) {
                    case 0 -> order.dish() + 1;
                    case 1 -> order.count();
                    case 2 -> (short) order.id();
                    default -> order.id() >>> 16;
                };
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
        for (int i = 0; i < BatchCookingBlockEntity.FOOD; i++) addSlot(new Slot(contents, i, 89 + i % 9 * 18, 109 + i / 9 * 18));
        for (int i = BatchCookingBlockEntity.FOOD; i < BatchCookingBlockEntity.SLOTS; i++) {
            int x = i < BatchCookingBlockEntity.RETURNS ? 89 + (i - BatchCookingBlockEntity.FOOD) * 18
                    : 206 + (i - BatchCookingBlockEntity.RETURNS) * 18;
            addSlot(new Slot(contents, i, x, 155) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int i = 0; i < 27; i++) addSlot(new Slot(inv, i + 9, 89 + i % 9 * 18, 182 + i / 9 * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, 89 + i * 18, 239));
        addDataSlots(data);
    }

    public int selection() { return data.get(0); }
    public BatchCookingBlockEntity.Status status() { return BatchCookingBlockEntity.Status.values()[data.get(1)]; }
    public int dish(int row) { return data.get(ORDER_DATA_START + row * ORDER_DATA_SIZE) - 1; }
    public int queued(int row) { return data.get(ORDER_DATA_START + row * ORDER_DATA_SIZE + 1); }
    public boolean selectedRecipeAvailable() { return data.get(2) != 0; }
    public boolean queueFull() { return queued(BatchCookingBlockEntity.QUEUE_LIMIT - 1) > 0; }

    public static int queueAction(int dish, int sizeIndex) { return QUEUE_START + dish * BATCH_SIZES.length + sizeIndex; }

    public int cancelAction(int row) {
        int offset = ORDER_DATA_START + row * ORDER_DATA_SIZE;
        int id = (data.get(offset + 3) << 16) | (data.get(offset + 2) & 0xffff);
        return -id;
    }

    private boolean recipeAvailable(int dish) {
        var world = kitchen.getLevel();
        return world != null && BatchCookingBlockEntity.recipe(world.getRecipeManager(), world.registryAccess(),
                CookingFoods.ALL.get(dish)) != null;
    }

    @Override public boolean stillValid(Player player) {
        if (kitchen == null) return true;
        return WorkstationAccess.stillValid(kitchen, player);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override public boolean clickMenuButton(Player player, int action) {
        if (kitchen == null || !stillValid(player) || player.isSpectator()) return false;
        if (action == SELECT_PREVIOUS || action == SELECT_NEXT) {
            selection = Math.floorMod(selection + (action == SELECT_NEXT ? 1 : -1), CookingFoods.ALL.size());
            return true;
        }
        if (action < 0) return kitchen.cancel(-action);
        int index = action - QUEUE_START;
        int dish = index / BATCH_SIZES.length;
        return dish < CookingFoods.ALL.size() && recipeAvailable(dish)
                && kitchen.enqueue(dish, BATCH_SIZES[index % BATCH_SIZES.length]);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(slotIndex);
        var stack = slot.getItem();
        if (stack.isEmpty() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var original = stack.copy();
        if (!(slotIndex < BatchCookingBlockEntity.SLOTS ? moveItemStackTo(stack, BatchCookingBlockEntity.SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, BatchCookingBlockEntity.FOOD, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
