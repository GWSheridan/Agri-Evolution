package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.GenomeBreeder;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.genetics.SpeciesBreeder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Contains both genetic parent stacks, two optional reagents and one output stack. */
public final class SeedBreedingStationBlockEntity extends BaseContainerBlockEntity {
    public static final int SEED_A = 0, SEED_B = 1, COMPOST = 2, SUPPORT = 3, OUTPUT = 4;
    public static final int BASE_TICKS = 200, COMPOST_TICKS = 140;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private ItemStack trackedA = ItemStack.EMPTY;
    private ItemStack trackedB = ItemStack.EMPTY;
    private int progress;
    private int greenhouseCheckTicks;
    private boolean greenhouseBoost;

    /** Vanilla menu data synchronization only transmits the two progress ints. */
    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return index == 0 ? progress : index == 1 ? requiredTicks() : 0;
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
        }
        @Override public int getCount() { return 2; }
    };

    public SeedBreedingStationBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.SEED_BREEDING_STATION.get(), pos, state);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.agri_evolution.seed_breeding_station");
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SeedBreedingStationMenu(id, inventory, this);
    }

    public int requiredTicks() {
        int base = getItem(COMPOST).is(AEItems.NUTRIENT_COMPOST.get()) ? COMPOST_TICKS : BASE_TICKS;
        return greenhouseBoost ? base * 4 / 5 : base;
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SEED_A, SEED_B -> SeedBreedingParents.accepts(stack);
            case COMPOST -> stack.is(AEItems.NUTRIENT_COMPOST.get());
            case SUPPORT -> stack.getItem() instanceof CropSupportItem;
            default -> false;
        };
    }

    private boolean sameParents() {
        return ItemStack.isSameItemSameComponents(trackedA, getItem(SEED_A))
            && ItemStack.isSameItemSameComponents(trackedB, getItem(SEED_B));
    }

    private void rememberParents() {
        trackedA = getItem(SEED_A).copy();
        trackedB = getItem(SEED_B).copy();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeedBreedingStationBlockEntity station) {
        if (++station.greenhouseCheckTicks >= 40) {
            station.greenhouseCheckTicks = 0;
            station.greenhouseBoost = com.agrievolution.farming.BotanicalGreenhouseLayout
                    .hasActiveGreenhouse(level, pos);
        }
        var pair = SeedBreedingParents.pair(station.getItem(SEED_A), station.getItem(SEED_B));
        boolean ready = pair == SeedBreedingParents.Pair.AGRI
                || pair == SeedBreedingParents.Pair.NATIVE && station.getItem(COMPOST).is(AEItems.NUTRIENT_COMPOST.get());
        if (!ready || !station.getItem(OUTPUT).isEmpty()) {
            if (station.progress != 0) { station.progress = 0; station.setChanged(); }
            station.rememberParents();
            return;
        }
        if (!station.sameParents()) {
            station.progress = 0;
            station.rememberParents();
        }
        station.progress++;
        station.setChanged();
        if (station.progress >= station.requiredTicks()) {
            if (pair == SeedBreedingParents.Pair.NATIVE) {
                ItemStack offspring = station.getItem(SEED_A).copyWithCount(1);
                station.getItem(COMPOST).shrink(1);
                station.setItem(OUTPUT, offspring);
            } else {
                station.breedAgriSeeds(level);
            }
            station.progress = 0;
            station.rememberParents();
            station.setChanged();
        }
    }

    private void breedAgriSeeds(Level level) {
        ItemStack parentA = getItem(SEED_A);
        ItemStack parentB = getItem(SEED_B);
        CropSpecies kindA = ((SpeciesSeedItem) parentA.getItem()).species();
        CropSpecies kindB = ((SpeciesSeedItem) parentB.getItem()).species();
        // Use the SAME algorithms as field breeding and the analyzer's preview.
        CropGenome genes = GenomeBreeder.cross(GenomeStorage.fromSeed(parentA), GenomeStorage.fromSeed(parentB),
                level.getRandom().nextLong());
        int bonus = getItem(SUPPORT).getCount() >= 2 ? CropSupportItem.fieldBonus(CropSupportItem.tier(getItem(SUPPORT))) : 0;
        CropSpecies species = SpeciesBreeder.cross(kindA, kindB, level.getRandom().nextLong(), bonus);
        ItemStack offspring = new ItemStack(AEItems.seedFor(species));
        com.agrievolution.genetics.SeedVariety.inherit(offspring, parentA, parentB);
        if (kindA != kindB) HybridLineageStorage.putOnSeed(offspring,
                new HybridLineageStorage.Lineage(kindA, kindB));
        else {
            var lineage = HybridLineageStorage.fromSeed(parentA);
            if (lineage == null) lineage = HybridLineageStorage.fromSeed(parentB);
            if (lineage != null) HybridLineageStorage.putOnSeed(offspring, lineage);
        }
        GenomeStorage.putOnSeed(offspring, genes);
        RareTraitStorage.putOnSeed(offspring,
                RareTrait.inherit(RareTraitStorage.fromSeed(parentA), RareTraitStorage.fromSeed(parentB),
                        new java.util.Random(level.getRandom().nextLong())));
        // Consume only AFTER an offspring item is completely constructed.
        parentA.shrink(1);
        parentB.shrink(1);
        if (getItem(COMPOST).is(AEItems.NUTRIENT_COMPOST.get())) getItem(COMPOST).shrink(1);
        if (bonus > 0 && SpeciesBreeder.mutationRecipe(kindA, kindB) != null) getItem(SUPPORT).shrink(2);
        setItem(OUTPUT, offspring);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BreedingProgress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = Math.max(0, Math.min(BASE_TICKS, tag.getInt("BreedingProgress")));
        rememberParents();
    }
}
