package com.agrievolution.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SeedVaultBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SIZE = 27, MASK_HALF = 14, HALF_MASK = (1 << MASK_HALF) - 1;
    private static final int[] NO_AUTOMATION_SLOTS = {};
    private NonNullList<ItemStack> seeds = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int locked;

    public SeedVaultBlockEntity(BlockPos pos, BlockState state) { super(AEBlockEntities.SEED_VAULT.get(), pos, state); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return seeds; }
    @Override protected void setItems(NonNullList<ItemStack> items) { seeds = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.seed_vault"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SeedVaultMenu(id, inventory, this); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return (locked & (1 << slot)) == 0 && SeedBagItem.accepts(stack); }
    @Override public int[] getSlotsForFace(Direction face) { return NO_AUTOMATION_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean stillValid(Player player) {
        return !isRemoved() && level != null && level.getBlockEntity(worldPosition) == this && super.stillValid(player);
    }

    int locks() { return locked; }
    void toggleLock(int slot) {
        locked ^= 1 << slot;
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, seeds, registries);
        tag.putInt("Locked", locked);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seeds = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, seeds, registries);
        locked = tag.getInt("Locked") & ((1 << SIZE) - 1);
    }
}
