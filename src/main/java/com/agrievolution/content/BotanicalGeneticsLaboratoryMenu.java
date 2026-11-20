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

/** Server-validated slot types and server-authoritative choice of research mode. */
public final class BotanicalGeneticsLaboratoryMenu extends AbstractContainerMenu {
    private static final int MACHINE = 5, PLAYER_START = 5, PLAYER_END = 41;
    private final Container lab;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private boolean closed;

    public BotanicalGeneticsLaboratoryMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE), new SimpleContainerData(4), ContainerLevelAccess.NULL);
    }
    public BotanicalGeneticsLaboratoryMenu(int id, Inventory inventory, BotanicalGeneticsLaboratoryBlockEntity entity) {
        this(id, inventory, entity, entity.data, ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos()));
    }
    private BotanicalGeneticsLaboratoryMenu(int id, Inventory inventory, Container lab,
                                            ContainerData data, ContainerLevelAccess access) {
        super(AEMenus.BOTANICAL_GENETICS_LABORATORY.get(), id);
        checkContainerSize(lab, MACHINE);
        checkContainerDataCount(data, 4);
        this.lab = lab;
        this.data = data;
        this.access = access;
        this.inventory = inventory;
        lab.startOpen(inventory.player);
        addSlot(new AccessSlot(lab, 0, 25, 83) {
            @Override public boolean mayPlace(ItemStack item) { return super.mayPlace(item) && item.getItem() instanceof SpeciesSeedItem; }
        });
        addSlot(new AccessSlot(lab, 1, 65, 83) {
            @Override public boolean mayPlace(ItemStack item) { return super.mayPlace(item) && item.is(AEItems.NUTRIENT_COMPOST.get()); }
        });
        addSlot(new AccessSlot(lab, 2, 105, 83) {
            @Override public boolean mayPlace(ItemStack item) {
                return super.mayPlace(item) && (GeneticSampleItem.isValid(item)
                        || (tier() >= 3 && mode() == BotanicalGeneticsLaboratoryBlockEntity.HYBRID
                        && item.getItem() instanceof SpeciesSeedItem));
            }
        });
        addSlot(new AccessSlot(lab, 3, 145, 83) {
            @Override public boolean mayPlace(ItemStack item) { return super.mayPlace(item) && item.getItem() instanceof CropSupportItem; }
        });
        addSlot(new AccessSlot(lab, 4, 185, 83) {
            @Override public boolean mayPlace(ItemStack item) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new AccessSlot(inventory, col + row * 9 + 9, 96 + col * 18, 147 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new AccessSlot(inventory, col, 96 + col * 18, 217));
        addDataSlots(data);
    }
    private class AccessSlot extends Slot {
        AccessSlot(Container contents, int index, int x, int y) { super(contents, index, x, y); }
        @Override public boolean mayPickup(Player player) { return stillValid(player); }
        @Override public boolean mayPlace(ItemStack item) { return stillValid(inventory.player); }
    }
    public int mode() { return data.get(2); }
    public int tier() { return data.get(3); }
    public int progress() { return data.get(0); }
    public int requiredTicks() { return Math.max(1, data.get(1)); }
    public ItemStack inputSeed() { return lab.getItem(0); }
    public ItemStack donorSeed() { return lab.getItem(2); }
    public ItemStack sample() { return lab.getItem(2); }
    public ItemStack support() { return lab.getItem(3); }
    public ItemStack compost() { return lab.getItem(1); }
    public boolean graftWouldImprove() {
        return BotanicalGeneticsLaboratoryBlockEntity.graftWouldImprove(inputSeed(), sample(), support(), tier() >= 2);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || button < 0 || button > BotanicalGeneticsLaboratoryBlockEntity.HYBRID
                || !(lab instanceof BotanicalGeneticsLaboratoryBlockEntity entity)
                || !WorkstationAccess.mayConfigure(entity, player) || !stillValid(player)) return false;
        if (button == BotanicalGeneticsLaboratoryBlockEntity.EVOLVE && !entity.isUpgraded()
                || button == BotanicalGeneticsLaboratoryBlockEntity.HYBRID && !entity.isHybridUpgraded()) return false;
        entity.selectMode(button);
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) {
        return !closed && player == inventory.player && (player == null ? !(lab instanceof BotanicalGeneticsLaboratoryBlockEntity)
                : player.isAlive() && !player.isSpectator() && lab.stillValid(player)
                    && stillValid(access, player, AEBlocks.BOTANICAL_GENETICS_LABORATORY.get()));
    }
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }
    @Override public void removed(Player player) {
        if (player != inventory.player) return;
        closed = true;
        super.removed(player);
        lab.stopOpen(player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (index < MACHINE) {
            if (!moveItemStackTo(current, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (current.getItem() instanceof SpeciesSeedItem) {
            // In hybrid mode allow shift-clicking a second species into the donor slot.
            if (mode() == BotanicalGeneticsLaboratoryBlockEntity.HYBRID && tier() >= 3
                    && lab.getItem(0).getItem() instanceof SpeciesSeedItem
                    && !lab.getItem(0).isEmpty()
                    && !moveItemStackTo(current, 2, 3, false)
                    && !moveItemStackTo(current, 0, 1, false)) return ItemStack.EMPTY;
            else if (mode() != BotanicalGeneticsLaboratoryBlockEntity.HYBRID || tier() < 3
                    || lab.getItem(0).isEmpty()) {
                if (!moveItemStackTo(current, 0, 1, false)) return ItemStack.EMPTY;
            }
        } else if (current.is(AEItems.NUTRIENT_COMPOST.get())) {
            if (!moveItemStackTo(current, 1, 2, false)) return ItemStack.EMPTY;
        } else if (GeneticSampleItem.isValid(current)) {
            if (!moveItemStackTo(current, 2, 3, false)) return ItemStack.EMPTY;
        } else if (current.getItem() instanceof CropSupportItem) {
            if (!moveItemStackTo(current, 3, 4, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START + 27) {
            if (!moveItemStackTo(current, PLAYER_START + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, PLAYER_START, PLAYER_START + 27, false)) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (current.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, current);
        return original;
    }
}
