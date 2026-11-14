package com.agrievolution.content;

import com.agrievolution.genetics.CropSpecies;
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

public final class SeedVaultMenu extends AbstractContainerMenu {
    public static final int SIZE = SeedVaultBlockEntity.SIZE, FIRST_X = 21, FIRST_Y = 57, PITCH = 18;
    public static final int SET_SPECIES = SIZE, CLEAR_SPECIES = SIZE + 1, CYCLE_MINIMUM = SIZE + 2;
    private static final int DATA_COUNT = 6;
    private final Container seeds;
    private final SeedVaultBlockEntity vault;
    private final ContainerData data;

    public SeedVaultMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SIZE), null, new SimpleContainerData(DATA_COUNT));
    }

    SeedVaultMenu(int id, Inventory inventory, SeedVaultBlockEntity vault) {
        this(id, inventory, vault, vault, new ContainerData() {
            @Override public int get(int index) {
                if (index < 2) return (vault.locks() >>> (index * SeedVaultBlockEntity.MASK_HALF)) & SeedVaultBlockEntity.HALF_MASK;
                return index == 2 ? vault.intakeSpeciesId() : vault.minimum(index - 3);
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        });
    }

    private SeedVaultMenu(int id, Inventory inventory, Container seeds, SeedVaultBlockEntity vault, ContainerData data) {
        super(AEMenus.SEED_VAULT.get(), id);
        this.seeds = seeds;
        this.vault = vault;
        this.data = data;
        addDataSlots(data);
        for (int i = 0; i < SIZE; i++) {
            int slot = i;
            addSlot(new Slot(seeds, slot, FIRST_X + slot % 9 * PITCH, FIRST_Y + slot / 9 * PITCH) {
                @Override public boolean mayPlace(ItemStack stack) { return !locked(slot) && SeedBagItem.accepts(stack); }
                @Override public boolean mayPickup(Player player) { return !locked(slot); }
            });
        }
        for (int i = 0; i < 27; i++) addSlot(new Slot(inventory, i + 9, FIRST_X + i % 9 * PITCH, 132 + i / 9 * PITCH));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, FIRST_X + i * PITCH, 190));
    }

    public boolean locked(int slot) {
        int half = slot / SeedVaultBlockEntity.MASK_HALF;
        return (data.get(half) & (1 << (slot % SeedVaultBlockEntity.MASK_HALF))) != 0;
    }
    public CropSpecies intakeSpecies() {
        int id = data.get(2);
        return id == 0 ? null : CropSpecies.values()[id - 1];
    }
    public int minimum(int trait) { return data.get(trait + 3); }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        int index = slots.indexOf(slot);
        return (index < 0 || index >= SIZE || !locked(index)) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override public boolean stillValid(Player player) { return vault == null || seeds.stillValid(player); }

    @Override public boolean clickMenuButton(Player player, int slot) {
        if (vault == null || player.isSpectator() || slot < 0 || slot >= CYCLE_MINIMUM + 3 || !stillValid(player)) return false;
        if (slot < SIZE) {
            if (!locked(slot) && seeds.getItem(slot).isEmpty()) return false;
            vault.toggleLock(slot);
        } else {
            if (!player.mayBuild() || !player.level().mayInteract(player, vault.getBlockPos())) return false;
            if (slot == SET_SPECIES) {
                if (!(getCarried().getItem() instanceof SpeciesSeedItem seed)) return false;
                vault.setIntakeSpecies(seed.species());
            } else if (slot == CLEAR_SPECIES) {
                if (!getCarried().isEmpty()) return false;
                vault.setIntakeSpecies(null);
            } else {
                vault.cycleMinimum(slot - CYCLE_MINIMUM);
            }
        }
        broadcastChanges();
        return true;
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (!stillValid(player) || (index >= 0 && index < SIZE && locked(index))) return;
        super.clicked(index, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!SeedBagItem.accepts(stack)) return ItemStack.EMPTY;
            for (int target = 0; target < SIZE && !stack.isEmpty(); target++) {
                var destination = getSlot(target);
                if (destination.hasItem() && destination.mayPlace(stack)
                        && ItemStack.isSameItemSameComponents(stack, destination.getItem())) {
                    destination.safeInsert(stack);
                }
            }
            for (int target = 0; target < SIZE && !stack.isEmpty(); target++) {
                var destination = getSlot(target);
                if (!destination.hasItem() && destination.mayPlace(stack)) destination.safeInsert(stack);
            }
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
