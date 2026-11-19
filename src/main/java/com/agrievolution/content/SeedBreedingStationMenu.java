package com.agrievolution.content;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Five station slots and the regular inventory, validated on the logical server. */
public final class SeedBreedingStationMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_FIRST = 5;
    private static final int PLAYER_END = PLAYER_FIRST + 36;
    private final Container station;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Inventory inventory;
    private boolean closed;

    /** Client uses a disposable inventory; network Slot updates fill it. */
    public SeedBreedingStationMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(2), ContainerLevelAccess.NULL);
    }

    /** Server uses the real block entity and synchronized progress. */
    public SeedBreedingStationMenu(int id, Inventory inventory, SeedBreedingStationBlockEntity station) {
        this(id, inventory, station, station.data, ContainerLevelAccess.create(station.getLevel(), station.getBlockPos()));
    }

    private SeedBreedingStationMenu(int id, Inventory inventory, Container station,
                                   ContainerData data, ContainerLevelAccess access) {
        super(AEMenus.SEED_BREEDING_STATION.get(), id);
        checkContainerSize(station, MACHINE_SLOTS);
        checkContainerDataCount(data, 2);
        this.station = station;
        this.access = access;
        this.data = data;
        this.inventory = inventory;
        station.startOpen(inventory.player);
        addSlot(new AccessSlot(station, 0, 41, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return super.mayPlace(stack) && SeedBreedingParents.accepts(stack); }
        });
        addSlot(new AccessSlot(station, 1, 81, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return super.mayPlace(stack) && SeedBreedingParents.accepts(stack); }
        });
        addSlot(new AccessSlot(station, 2, 152, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return super.mayPlace(stack) && stack.is(AEItems.NUTRIENT_COMPOST.get()); }
        });
        addSlot(new AccessSlot(station, 3, 192, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return super.mayPlace(stack) && stack.getItem() instanceof CropSupportItem; }
        });
        addSlot(new AccessSlot(station, 4, 296, 57) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new AccessSlot(inventory, col + row * 9 + 9, 96 + col * 18, 132 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new AccessSlot(inventory, col, 96 + col * 18, 202));
        addDataSlots(data);
    }

    private class AccessSlot extends Slot {
        AccessSlot(Container contents, int index, int x, int y) { super(contents, index, x, y); }
        @Override public boolean mayPickup(Player player) { return stillValid(player); }
        @Override public boolean mayPlace(ItemStack stack) { return stillValid(inventory.player); }
    }

    public int progress() { return data.get(0); }
    public int maxProgress() { return Math.max(1, data.get(1)); }
    public ItemStack parentA() { return station.getItem(0); }
    public ItemStack parentB() { return station.getItem(1); }
    public boolean hasCompost() { return station.getItem(2).is(AEItems.NUTRIENT_COMPOST.get()); }
    public boolean hasOffspring() { return !station.getItem(4).isEmpty(); }
    public int bonus() { return station.getItem(3).getCount()>=2 ? CropSupportItem.fieldBonus(CropSupportItem.tier(station.getItem(3))) : 0; }
    public boolean hasBonus() { return station.getItem(3).getCount() >= 2; }

    @Override
    public boolean stillValid(Player player) {
        return !closed && player == inventory.player && (player == null ? !(station instanceof SeedBreedingStationBlockEntity)
                : player.isAlive() && !player.isSpectator() && station.stillValid(player)
                    && stillValid(access, player, AEBlocks.SEED_BREEDING_STATION.get()));
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override
    public void removed(Player player) {
        if (player != inventory.player) return;
        closed = true;
        super.removed(player);
        station.stopOpen(player); // Inventory stays in the workstation when GUI closes.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot clicked = slots.get(index);
        if (!clicked.hasItem() || !clicked.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = clicked.getItem();
        ItemStack original = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, PLAYER_FIRST, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (SeedBreedingParents.accepts(current)) {
            if (!moveItemStackTo(current, 0, 2, false)) return ItemStack.EMPTY;
        } else if (current.is(AEItems.NUTRIENT_COMPOST.get())) {
            if (!moveItemStackTo(current, 2, 3, false)) return ItemStack.EMPTY;
        } else if (current.getItem() instanceof CropSupportItem) {
            if (!moveItemStackTo(current, 3, 4, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_FIRST + 27) {
            if (!moveItemStackTo(current, PLAYER_FIRST + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, PLAYER_FIRST, PLAYER_FIRST + 27, false)) return ItemStack.EMPTY;
        if (current.isEmpty()) clicked.set(ItemStack.EMPTY);
        else clicked.setChanged();
        if (current.getCount() == original.getCount()) return ItemStack.EMPTY;
        clicked.onTake(player, current);
        return original;
    }
}

