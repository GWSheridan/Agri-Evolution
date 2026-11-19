package com.agrievolution.content;

import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeStorage;
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
    private CropSpecies intakeSpecies;
    private final int[] minimum = new int[3];

    public SeedVaultBlockEntity(BlockPos pos, BlockState state) { super(AEBlockEntities.SEED_VAULT.get(), pos, state); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return seeds; }
    @Override protected void setItems(NonNullList<ItemStack> items) { seeds = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.agri_evolution.seed_vault"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new SeedVaultMenu(id, inventory, this); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < SIZE && WorkstationAccess.isCurrent(this)
                && (locked & (1 << slot)) == 0 && SeedBagItem.accepts(stack);
    }
    @Override public int[] getSlotsForFace(Direction face) { return NO_AUTOMATION_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    @Override public boolean stillValid(Player player) {
        return player != null && player.level() == level && WorkstationAccess.isCurrent(this) && super.stillValid(player);
    }

    int locks() { return locked; }
    int intakeSpeciesId() { return intakeSpecies == null ? 0 : intakeSpecies.ordinal() + 1; }
    int minimum(int trait) { return minimum[trait]; }

    void setIntakeSpecies(CropSpecies species) {
        if (!WorkstationAccess.isCurrent(this)) return;
        intakeSpecies = species;
        setChanged();
    }

    void cycleMinimum(int trait) {
        if (trait < 0 || trait >= minimum.length || !WorkstationAccess.isCurrent(this)) return;
        minimum[trait] = (minimum[trait] + 10) % 110;
        setChanged();
    }

    boolean receiveOffspring(ItemStack stack) {
        if (!WorkstationAccess.isCurrent(this) || !SeedBagItem.accepts(stack)) return false;
        if (intakeSpecies != null || minimum[0] != 0 || minimum[1] != 0 || minimum[2] != 0) {
            if (!(stack.getItem() instanceof SpeciesSeedItem seed)) return false;
            if (intakeSpecies != null && seed.species() != intakeSpecies) return false;
            var genes = GenomeStorage.fromSeed(stack);
            if (genes.growth() < minimum[0] || genes.yield() < minimum[1] || genes.resilience() < minimum[2]) return false;
        }
        int empty = -1;
        for (int slot = 0; slot < SIZE; slot++) {
            if (!canPlaceItem(slot, stack)) continue;
            var stored = seeds.get(slot);
            if (stored.isEmpty()) {
                if (empty < 0) empty = slot;
            } else if (stored.getCount() < stored.getMaxStackSize() && ItemStack.isSameItemSameComponents(stored, stack)) {
                stored.grow(1);
                setChanged();
                return true;
            }
        }
        if (empty < 0) return false;
        setItem(empty, stack.copyWithCount(1));
        return true;
    }
    void toggleLock(int slot) {
        if (slot < 0 || slot >= SIZE || !WorkstationAccess.isCurrent(this)) return;
        locked ^= 1 << slot;
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, seeds, registries);
        tag.putInt("Locked", locked);
        if (intakeSpecies != null) tag.putString("IntakeSpecies", intakeSpecies.name());
        tag.putIntArray("IntakeMinimum", minimum);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seeds = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, seeds, registries);
        locked = tag.getInt("Locked") & ((1 << SIZE) - 1);
        intakeSpecies = null;
        if (tag.contains("IntakeSpecies")) {
            try { intakeSpecies = CropSpecies.valueOf(tag.getString("IntakeSpecies")); }
            catch (IllegalArgumentException ignored) { }
        }
        var savedMinimum = tag.getIntArray("IntakeMinimum");
        for (int i = 0; i < minimum.length; i++) {
            minimum[i] = i < savedMinimum.length ? Math.clamp(savedMinimum[i], 0, 100) / 10 * 10 : 0;
        }
    }
}
