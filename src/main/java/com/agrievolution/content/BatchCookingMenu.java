package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BatchCookingMenu extends AbstractContainerMenu {
    public static final int SELECT_PREVIOUS = 0, SELECT_NEXT = 1, QUEUE_ONE = 2, QUEUE_FOUR = 3, QUEUE_SIXTEEN = 4;
    public static final int CANCEL_START = 100;
    private static final int DATA_COUNT = 2 + BatchCookingBlockEntity.QUEUE_LIMIT * 2;
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
                var order = kitchen.order((index - 2) / 2);
                return order == null ? 0 : (index % 2 == 0 ? order.dish() + 1 : order.count());
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
    public int dish(int row) { return data.get(2 + row * 2) - 1; }
    public int queued(int row) { return data.get(3 + row * 2); }

    public int cancelAction(int row) { return CANCEL_START + (row * CookingFoods.ALL.size() + dish(row)) * 16 + queued(row) - 1; }

    @Override public boolean stillValid(Player player) {
        if (kitchen == null) return true;
        return !kitchen.isRemoved() && stillValid(ContainerLevelAccess.create(kitchen.getLevel(), kitchen.getBlockPos()),
                player, AEBlocks.BATCH_COOKING_STATION.get());
    }

    @Override public boolean clickMenuButton(Player player, int action) {
        if (kitchen == null || !stillValid(player) || player.isSpectator()) return false;
        if (action == SELECT_PREVIOUS || action == SELECT_NEXT) {
            selection = Math.floorMod(selection + (action == SELECT_NEXT ? 1 : -1), CookingFoods.ALL.size());
            return true;
        }
        if (action >= QUEUE_ONE && action <= QUEUE_SIXTEEN) {
            return kitchen.enqueue(selection, 1 << ((action - QUEUE_ONE) * 2));
        }
        if (action < CANCEL_START || action >= CANCEL_START + BatchCookingBlockEntity.QUEUE_LIMIT * CookingFoods.ALL.size() * 16) return false;
        int index = action - CANCEL_START;
        int dish = index / 16 % CookingFoods.ALL.size();
        return kitchen.cancel(index / 16 / CookingFoods.ALL.size(), dish, index % 16 + 1);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(slotIndex);
        var stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var original = stack.copy();
        if (!(slotIndex < BatchCookingBlockEntity.SLOTS ? moveItemStackTo(stack, BatchCookingBlockEntity.SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, BatchCookingBlockEntity.FOOD, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
