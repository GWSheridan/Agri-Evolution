package com.agrievolution.content;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** Binds a portable menu to its owner, held container and last saved contents. */
final class PortableStorageAccess {
    private final Inventory inventory;
    private final int slot;
    private final ItemStack stack;
    private final Class<? extends Item> type;
    private final Level openedLevel;
    private ItemContainerContents saved;
    private boolean closed;

    PortableStorageAccess(Inventory inventory, int slot, Class<? extends Item> type) {
        this.inventory = inventory;
        this.slot = slot;
        this.type = type;
        openedLevel = inventory.player == null ? null : inventory.player.level();
        stack = slot >= 0 && slot < inventory.getContainerSize() ? inventory.getItem(slot) : ItemStack.EMPTY;
        saved = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    ItemStack stack() { return stack; }

    boolean current(Player player) {
        if (closed || player != inventory.player) return false;
        // The world-free GUI preview has no player and cannot write an item inventory.
        if (openedLevel == null) return player == null;
        return player.isAlive() && !player.isSpectator() && player.level() == openedLevel && (openedLevel.isClientSide
                || type.isInstance(stack.getItem()) && slot >= 0 && slot < inventory.getContainerSize()
                && inventory.getItem(slot) == stack
                && saved.equals(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));
    }

    void save(List<ItemStack> contents) {
        if (openedLevel != null && !openedLevel.isClientSide && current(inventory.player)) {
            // Imported containers may contain extra slots. Editing the visible
            // nine slots must not erase those saved entries.
            var merged = new java.util.ArrayList<>(contents);
            for (int index = contents.size(); index < saved.getSlots(); index++) merged.add(saved.getStackInSlot(index));
            saved = ItemContainerContents.fromItems(merged);
            stack.set(DataComponents.CONTAINER, saved);
            inventory.setChanged();
        }
    }

    boolean ownedBy(Player player) { return player == inventory.player; }
    void close() { closed = true; }
}
