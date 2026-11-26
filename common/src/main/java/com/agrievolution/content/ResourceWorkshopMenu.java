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

import static com.agrievolution.content.ResourceWorkshopBlockEntity.*;

public final class ResourceWorkshopMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 4;
    private final ResourceWorkshopBlockEntity workshop;
    private final ContainerData data;
    private final Player owner;
    private boolean closed;

    public ResourceWorkshopMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    ResourceWorkshopMenu(int id, Inventory inventory, ResourceWorkshopBlockEntity workshop) {
        super(AEMenus.RESOURCE_WORKSHOP.get(), id);
        this.workshop = workshop;
        owner = inventory.player;
        Container contents = workshop == null ? new SimpleContainer(SLOTS) : workshop;
        data = workshop == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> workshop.tier();
                    case 1 -> workshop.status().ordinal();
                    case 2 -> workshop.progress();
                    case 3 -> workshop.duration();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addSlot(new WorkshopSlot(contents, MATERIAL, 51, 106));
        addSlot(new WorkshopSlot(contents, BINDER, 101, 106));
        addSlot(new WorkshopSlot(contents, OUTPUT, 186, 106) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new WorkshopSlot(inventory, 9 + row * 9 + column, 46 + column * 18, 157 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new WorkshopSlot(inventory, column, 46 + column * 18, 213));
        }
        addDataSlots(data);
    }

    private class WorkshopSlot extends Slot {
        WorkshopSlot(Container contents, int index, int x, int y) { super(contents, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return stillValid(owner); }
        @Override public boolean mayPickup(Player player) { return stillValid(player); }
    }

    public int tier() { return Math.max(1, data.get(0)); }
    public Status status() { return Status.values()[data.get(1)]; }
    public float progress() {
        int duration = data.get(3);
        return duration == 0 ? 0 : Math.clamp((float) data.get(2) / duration, 0, 1);
    }

    @Override public boolean stillValid(Player player) {
        return !closed && player != null && player == owner && player.isAlive() && !player.isSpectator()
                && (workshop == null || WorkstationAccess.mayConfigure(workshop, player));
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override public void removed(Player player) {
        if (player != owner) return;
        closed = true;
        super.removed(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        var slot = getSlot(slotIndex);
        var stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        var original = stack.copy();
        if (slotIndex < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            var recipes = player.level().getRecipeManager().getAllRecipesFor(AERecipes.RESOURCE_WORKSHOP.get());
            var material = getSlot(MATERIAL).getItem();
            var binder = getSlot(BINDER).getItem();
            boolean primary = recipes.stream().anyMatch(holder -> holder.value().material().test(stack)
                    && (binder.isEmpty() || holder.value().binder().test(binder)));
            boolean secondary = recipes.stream().anyMatch(holder -> holder.value().binder().test(stack)
                    && (material.isEmpty() || holder.value().material().test(material)));
            if (!(primary && moveItemStackTo(stack, MATERIAL, MATERIAL + 1, false))
                    && !(secondary && moveItemStackTo(stack, BINDER, BINDER + 1, false))) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
