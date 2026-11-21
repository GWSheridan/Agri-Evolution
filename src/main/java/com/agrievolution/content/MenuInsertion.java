package com.agrievolution.content;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Checks the click's inputs before vanilla caches them for an insertion. */
final class MenuInsertion {
    static boolean applies(int index, int storageSlots, ClickType type) {
        return type == ClickType.QUICK_CRAFT || index >= 0 && index < storageSlots && (type == ClickType.PICKUP || type == ClickType.SWAP);
    }
    private final AbstractContainerMenu menu;
    private final Inventory inventory;
    private final int source;
    private final Slot target;
    private final ItemStack cursor, beforeCursor, input, beforeInput, destination, beforeDestination;

    MenuInsertion(AbstractContainerMenu menu, Inventory inventory, int index, int button, ClickType type) {
        this.menu = menu;
        this.inventory = inventory;
        source = type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40) ? button : -1;
        cursor = menu.getCarried();
        beforeCursor = cursor.copy();
        input = source < 0 ? cursor : inventory.getItem(source);
        beforeInput = input.copy();
        target = index >= 0 && index < menu.slots.size() ? menu.getSlot(index) : null;
        destination = target == null ? ItemStack.EMPTY : target.getItem();
        beforeDestination = destination.copy();
    }

    ItemStack input() { return input; }

    boolean current() {
        return menu.getCarried() == cursor && ItemStack.matches(beforeCursor, cursor)
                && (source < 0 || inventory.getItem(source) == input) && ItemStack.matches(beforeInput, input)
                && (target == null || target.getItem() == destination && ItemStack.matches(beforeDestination, destination));
    }
}
