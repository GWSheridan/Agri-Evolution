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
import net.minecraft.world.item.Items;

/** Five slots: sampler, specimen, farmland input, catalyst and protected output. */
public final class SoilResearchTableMenu extends AbstractContainerMenu {
    private static final int MACHINE = 5, PLAYER_FIRST = 5, PLAYER_END = 41;
    private final Container station;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final SoilResearchTableBlockEntity backend;

    public SoilResearchTableMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE), new SimpleContainerData(4), ContainerLevelAccess.NULL, null);
    }
    public SoilResearchTableMenu(int id, Inventory inventory, SoilResearchTableBlockEntity entity) {
        this(id, inventory, entity, entity.data,
                ContainerLevelAccess.create(entity.getLevel(), entity.getBlockPos()), entity);
    }
    private SoilResearchTableMenu(int id, Inventory inv, Container station, ContainerData data,
                                  ContainerLevelAccess access, SoilResearchTableBlockEntity backend) {
        super(AEMenus.SOIL_RESEARCH_TABLE.get(), id);
        checkContainerSize(station, MACHINE);
        checkContainerDataCount(data, 4);
        this.station = station;
        this.data = data;
        this.access = access;
        this.backend = backend;
        station.startOpen(inv.player);
        addSlot(new Slot(station, 0, 30, 62) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(AEItems.SOIL_SAMPLE.get()); }
        });
        addSlot(new Slot(station, 1, 83, 62) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(AEItems.SOIL_SPECIMEN.get()); }
        });
        addSlot(new Slot(station, 2, 152, 62) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.DIRT) || stack.is(AEItems.CULTIVATED_FARMLAND.get())
                        || stack.is(AEItems.NUTRIENT_FARMLAND.get()) || stack.is(AEItems.MYTHIC_FARMLAND.get())
                        || stack.is(AEItems.ARCANE_FARMLAND.get());
            }
        });
        addSlot(new Slot(station, 3, 202, 62) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(AEItems.NUTRIENT_COMPOST.get()) || stack.is(Items.COPPER_INGOT)
                        || stack.is(Items.AMETHYST_SHARD) || stack.is(Items.GOLD_INGOT)
                        || stack.is(Items.ECHO_SHARD);
            }
        });
        addSlot(new Slot(station, 4, 294, 62) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlot(new Slot(inv, c + 9 + r * 9, 94 + c * 18, 159 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 94 + c * 18, 229));
        addDataSlots(data);
    }
    public int tableTier() { return data.get(0); }
    public int analyzed() { return data.get(1); }
    public int status() { return data.get(2); }
    public int lastBatch() { return data.get(3); }
    public ItemStack sampler() { return station.getItem(0); }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button > 1 || backend == null || !WorkstationAccess.mayConfigure(backend, player)) return false;
        if (button == 0) backend.analyze(player);
        else backend.refine(player);
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return station.stillValid(player) && stillValid(access, player, AEBlocks.SOIL_RESEARCH_TABLE.get());
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (stillValid(player)) super.clicked(index, button, type, player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        station.stopOpen(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack copy = held.copy();
        if (index < MACHINE) {
            if (!moveItemStackTo(held, PLAYER_FIRST, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            int destination = held.is(AEItems.SOIL_SAMPLE.get()) ? 0 : held.is(AEItems.SOIL_SPECIMEN.get()) ? 1
                    : slots.get(2).mayPlace(held) ? 2 : slots.get(3).mayPlace(held) ? 3 : -1;
            if (destination < 0) {
                if (index < PLAYER_FIRST + 27) {
                    if (!moveItemStackTo(held, PLAYER_FIRST + 27, PLAYER_END, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(held, PLAYER_FIRST, PLAYER_FIRST + 27, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(held, destination, destination + 1, false)) return ItemStack.EMPTY;
        }
        if (held.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (held.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, held);
        return copy;
    }
}
