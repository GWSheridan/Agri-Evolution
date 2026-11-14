package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 27 output-only slots in the upgraded controller and a normal player inventory. */
public final class BotanicalGreenhouseStorageMenu extends AbstractContainerMenu {
    private final Container storage;

    public BotanicalGreenhouseStorageMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(BotanicalGreenhouseBlockEntity.STORAGE_SIZE));
    }

    public BotanicalGreenhouseStorageMenu(int id, Inventory inventory, Container storage) {
        super(AEMenus.BOTANICAL_GREENHOUSE_STORAGE.get(), id);
        this.storage = storage;
        checkContainerSize(storage, BotanicalGreenhouseBlockEntity.STORAGE_SIZE);
        storage.startOpen(inventory.player);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(storage, col + row * 9, 17 + col * 18, 47 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col + row * 9 + 9, 17 + col * 18, 118 + row * 18));
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 17 + col * 18, 182));
        }
    }

    @Override public boolean stillValid(Player player) { return storage.stillValid(player); }

    @Override public void removed(Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Output-only. Never permit player inventory to insert crops or items into storage.
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
