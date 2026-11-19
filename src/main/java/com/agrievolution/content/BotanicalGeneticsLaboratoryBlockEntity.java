package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.GeneticResearch;
import com.agrievolution.genetics.AdvancedHybridBreeder;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.CropSpecies;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.RareTraitStorage;
import com.agrievolution.farming.BotanicalGreenhouseLayout;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Five slots: primary seed, compost, donor DNA/second seed, optional support, result. */
public final class BotanicalGeneticsLaboratoryBlockEntity extends BaseContainerBlockEntity {
    public static final int SEED = 0, COMPOST = 1, SAMPLE = 2, SUPPORT = 3, OUTPUT = 4;
    public static final int GROWTH = 0, YIELD = 1, RESILIENCE = 2, EXTRACT = 3, GRAFT = 4;
    public static final int EVOLVE = 5, HYBRID = 6;
    public static final int TICKS = 240;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private ItemStack trackedSeed = ItemStack.EMPTY;
    private ItemStack trackedSample = ItemStack.EMPTY;
    private int progress;
    private int mode;
    private int checkTicks;
    private boolean greenhouseBoost;
    private boolean upgraded;
    private boolean hybridUpgraded;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> requiredTicks();
                case 2 -> mode;
                case 3 -> hybridUpgraded ? 3 : upgraded ? 2 : 1;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 2) mode = Math.max(0, Math.min(HYBRID, value));
            if (index == 3) { upgraded = value >= 2; hybridUpgraded = value >= 3; }
        }
        @Override public int getCount() { return 4; }
    };

    public BotanicalGeneticsLaboratoryBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.BOTANICAL_GENETICS_LABORATORY.get(), pos, state);
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.agri_evolution.botanical_genetics_laboratory");
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new BotanicalGeneticsLaboratoryMenu(id, inventory, this);
    }

    @Override public boolean stillValid(Player player) { return WorkstationAccess.stillValid(this, player); }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SEED -> stack.getItem() instanceof SpeciesSeedItem;
            case COMPOST -> stack.is(AEItems.NUTRIENT_COMPOST.get());
            case SAMPLE -> GeneticSampleItem.isValid(stack)
                    || hybridUpgraded && mode == HYBRID && stack.getItem() instanceof SpeciesSeedItem;
            case SUPPORT -> stack.getItem() instanceof CropSupportItem;
            default -> false;
        };
    }
    public int mode() { return mode; }
    public boolean isUpgraded() { return upgraded; }
    public boolean isHybridUpgraded() { return hybridUpgraded; }
    public boolean installHybridUpgrade() {
        if (!upgraded || hybridUpgraded) return false;
        hybridUpgraded = true;
        progress = 0;
        setChanged();
        return true;
    }
    public boolean installUpgrade() {
        if (upgraded) return false;
        upgraded = true;
        setChanged();
        return true;
    }
    public void selectMode(int newMode) {
        if (newMode >= GROWTH && newMode <= HYBRID && newMode != mode
                && (newMode != EVOLVE || upgraded) && (newMode != HYBRID || hybridUpgraded)) {
            mode = newMode;
            progress = 0;
            setChanged();
        }
    }
    private int selectedGene(CropGenome gene) {
        return switch (mode) {
            case GROWTH -> gene.growth();
            case YIELD -> gene.yield();
            default -> gene.resilience();
        };
    }
    public int requiredTicks() {
        int ticks = greenhouseBoost ? TICKS * 4 / 5 : TICKS;
        if (mode == HYBRID) return greenhouseBoost ? 256 : 320;
        return upgraded ? ticks * 9 / 10 : ticks;
    }

    public static boolean graftWouldImprove(ItemStack seed, ItemStack sample, ItemStack support, boolean upgraded) {
        if (!(seed.getItem() instanceof SpeciesSeedItem) || !GeneticSampleItem.isValid(sample)) return false;
        var original = GenomeStorage.fromSeed(seed);
        var grafted = GeneticResearch.graft(original, GeneticSampleItem.genome(sample), CropSupportItem.tier(support) > 0);
        return !original.equals(grafted) || upgraded && RareTraitStorage.fromSeed(seed) == RareTrait.NONE
                && GeneticSampleItem.rareTrait(sample) != RareTrait.NONE;
    }

    private boolean canWork() {
        if (!(getItem(SEED).getItem() instanceof SpeciesSeedItem)
                || !getItem(COMPOST).is(AEItems.NUTRIENT_COMPOST.get())
                || !getItem(OUTPUT).isEmpty()) return false;
        if (mode == HYBRID) {
            if (!hybridUpgraded || !(getItem(SAMPLE).getItem() instanceof SpeciesSeedItem donor)) return false;
            CropSpecies a = ((SpeciesSeedItem) getItem(SEED).getItem()).species();
            return a != donor.species();
        }
        if (mode == GRAFT) return graftWouldImprove(getItem(SEED), getItem(SAMPLE), getItem(SUPPORT), upgraded);
        if (mode == EVOLVE) return upgraded && GeneticSampleItem.isValid(getItem(SAMPLE))
                && RareTraitStorage.fromSeed(getItem(SEED)) == RareTrait.NONE;
        // Prevent consuming compost to produce a seed that cannot improve any further.
        if (mode <= RESILIENCE) return selectedGene(GenomeStorage.fromSeed(getItem(SEED))) < 100;
        return true;
    }
    private void trackInputs() {
        trackedSeed = getItem(SEED).copy();
        trackedSample = getItem(SAMPLE).copy();
    }
    private boolean sameInputs() {
        return ItemStack.isSameItemSameComponents(trackedSeed, getItem(SEED))
            && ItemStack.isSameItemSameComponents(trackedSample, getItem(SAMPLE));
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  BotanicalGeneticsLaboratoryBlockEntity lab) {
        if (++lab.checkTicks >= 40) {
            lab.checkTicks = 0;
            lab.greenhouseBoost = BotanicalGreenhouseLayout.hasActiveGreenhouse(level, pos);
        }
        if (!lab.canWork()) {
            if (lab.progress != 0) { lab.progress = 0; lab.setChanged(); }
            lab.trackInputs();
            return;
        }
        if (!lab.sameInputs()) {
            lab.progress = 0;
            lab.trackInputs();
        }
        lab.progress++;
        if (lab.progress >= lab.requiredTicks()) {
            lab.process();
            lab.progress = 0;
            lab.trackInputs();
            lab.setChanged();
        } else if (lab.progress % 10 == 0) lab.setChanged();
    }
    private void process() {
        if (!canWork()) return;
        ItemStack seed = getItem(SEED);
        CropGenome before = GenomeStorage.fromSeed(seed);
        ItemStack result;
        boolean support = CropSupportItem.tier(getItem(SUPPORT))>0;
        if (mode == EXTRACT) {
            result = GeneticSampleItem.capture(seed);
        } else if (mode == HYBRID) {
            ItemStack donor = getItem(SAMPLE);
            CropSpecies a = ((SpeciesSeedItem) seed.getItem()).species();
            CropSpecies b = ((SpeciesSeedItem) donor.getItem()).species();
            boolean boosted = support;
            CropSpecies offspring = AdvancedHybridBreeder.crossSpecies(a, b,
                    level.getRandom().nextLong(), CropSupportItem.labBonus(CropSupportItem.tier(getItem(SUPPORT))), greenhouseBoost);
            result = new ItemStack(AEItems.seedFor(offspring));
            HybridLineageStorage.putOnSeed(result, new HybridLineageStorage.Lineage(a, b));
            GenomeStorage.putOnSeed(result, AdvancedHybridBreeder.crossGenes(
                    before, GenomeStorage.fromSeed(donor), level.getRandom().nextLong(), boosted));
            RareTraitStorage.putOnSeed(result, RareTrait.inherit(
                    RareTraitStorage.fromSeed(seed), RareTraitStorage.fromSeed(donor),
                    new Random(level.getRandom().nextLong())));
        } else {
            result = seed.copyWithCount(1); // Keep species and all other custom data.
            CropGenome after;
            if (mode == EVOLVE) {
                // One seed and one donor sample form a fresh experimental line.
                // The experiment always boosts baseline genetics; a rare discovery is probabilistic.
                after = new CropGenome(before.growth() + 2, before.yield() + 2, before.resilience() + 2);
                int chance = 20 + (support ? 15 : 0) + (greenhouseBoost ? 10 : 0);
                Random rng = new Random(level.getRandom().nextLong());
                if (rng.nextInt(100) < chance) {
                    RareTraitStorage.putOnSeed(result, RareTrait.discover(rng));
                }
            } else if (mode == GRAFT) {
                CropGenome sample = GeneticSampleItem.genome(getItem(SAMPLE));
                // The chosen sample improves a weak seed only up to the donor value.
                // A support additionally gives a small, capped stability bonus.
                after = GeneticResearch.graft(before, sample, support);
                if (RareTraitStorage.fromSeed(result) == RareTrait.NONE && upgraded)
                    RareTraitStorage.putOnSeed(result, GeneticSampleItem.rareTrait(getItem(SAMPLE)));
            } else {
                after = GeneticResearch.enhance(before, mode, support);
            }
            GenomeStorage.putOnSeed(result, after);
        }
        // Committed only once the result is successfully constructed.
        seed.shrink(1);
        getItem(COMPOST).shrink(1);
        if (mode == GRAFT || mode == EVOLVE || mode == HYBRID) getItem(SAMPLE).shrink(1);
        if (support && mode != EXTRACT) getItem(SUPPORT).shrink(1);
        setItem(OUTPUT, result);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("ResearchProgress", progress);
        tag.putInt("ResearchMode", mode);
        tag.putBoolean("AdvancedResearchUpgrade", upgraded);
        tag.putBoolean("HybridizationUpgrade", hybridUpgraded);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = Math.max(0, Math.min(320, tag.getInt("ResearchProgress")));
        upgraded = tag.getBoolean("AdvancedResearchUpgrade");
        hybridUpgraded = upgraded && tag.getBoolean("HybridizationUpgrade");
        mode = Math.max(GROWTH, Math.min(HYBRID, tag.getInt("ResearchMode")));
        if ((!upgraded && mode == EVOLVE) || (!hybridUpgraded && mode == HYBRID)) mode = GROWTH;
        trackInputs();
    }
}
