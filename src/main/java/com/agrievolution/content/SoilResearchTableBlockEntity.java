package com.agrievolution.content;

import com.agrievolution.farming.SoilResearch;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Server-side five-slot processing inventory. Old sampler CUSTOM_DATA remains authoritative. */
public final class SoilResearchTableBlockEntity extends BaseContainerBlockEntity {
    public static final int SAMPLER = 0, SPECIMEN = 1, SOIL = 2, CATALYST = 3, OUTPUT = 4;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int status = 0;
    private int lastBatch = 0;
    public void setStatus(int value) { status = value; setChanged(); }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return i == 0 ? getBlockState().getValue(SoilResearchTableBlock.TIER)
                    : i == 1 ? SoilResearch.analyzed(getItem(SAMPLER)) : i == 2 ? status : i == 3 ? lastBatch : 0;
        }
        @Override public void set(int i, int v) { /* State and item NBT are server authoritative. */ }
        @Override public int getCount() { return 4; }
    };

    public SoilResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.SOIL_RESEARCH_TABLE.get(), pos, state);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> stacks) { items = stacks; }
    @Override protected Component getDefaultName() {
        return Component.translatable("container.agri_evolution.soil_research_table");
    }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new SoilResearchTableMenu(id, inventory, this);
    }

    /** Mk II/Mk III research up to 2/4 collected specimens in a single click. */
    public void analyze(Player player) {
        ItemStack tool = getItem(SAMPLER), specimen = getItem(SPECIMEN);
        if (!tool.is(AEItems.SOIL_SAMPLE.get()) || !specimen.is(AEItems.SOIL_SPECIMEN.get())) {
            setStatus(!tool.is(AEItems.SOIL_SAMPLE.get()) ? 1 : 2);
            return;
        }
        int tableTier = data.get(0);
        int oldCount = SoilResearch.analyzed(tool);
        int limit = tableTier == 1 ? 48 : tableTier == 2 ? 72 : 96;
        if (oldCount >= limit) {
            setStatus(oldCount >= 96 ? 4 : 9);
            return;
        }
        int batchLimit = tableTier == 1 ? 1 : tableTier == 2 ? 2 : 4;
        int maximum = Math.min(batchLimit, limit - oldCount);
        if (!player.getAbilities().instabuild) maximum = Math.min(maximum, specimen.getCount());
        int studied = 0;
        for (int i = 0; i < maximum; i++) {
            if (!SoilResearch.analyze(tool, specimen)) break;
            studied++;
            if (!player.getAbilities().instabuild) specimen.shrink(1);
        }
        if (studied == 0) {
            setStatus(3);
            return;
        }
        lastBatch = studied;
        FarmingMilestones.SOIL.award(player);
        setChanged();
        setStatus(5);
    }

    public void refine(Player player) {
        ItemStack tool = getItem(SAMPLER), source = getItem(SOIL), feed = getItem(CATALYST);
        if (!tool.is(AEItems.SOIL_SAMPLE.get())) {
            setStatus(1);
            return;
        }
        if (!getItem(OUTPUT).isEmpty()) {
            setStatus(6);
            return;
        }
        int previous = source.is(Items.DIRT) ? 0
                : source.is(AEItems.CULTIVATED_FARMLAND.get()) ? 1
                : source.is(AEItems.NUTRIENT_FARMLAND.get()) ? 2
                : source.is(AEItems.MYTHIC_FARMLAND.get()) ? 3
                : source.is(AEItems.ARCANE_FARMLAND.get()) ? 4 : -1;
        if (previous < 0) {
            setStatus(7);
            return;
        }
        int requiredTier = previous + 1;
        int requiredTable = requiredTier <= 3 ? 1 : requiredTier == 4 ? 2 : 3;
        if (data.get(0) < requiredTable) {
            setStatus(9);
            return;
        }
        if (SoilResearch.researchedTier(tool) < requiredTier) {
            setStatus(10);
            return;
        }
        Item catalyst = switch (previous) {
            case 0 -> AEItems.NUTRIENT_COMPOST.get();
            case 1 -> Items.COPPER_INGOT;
            case 2 -> Items.AMETHYST_SHARD;
            case 3 -> Items.GOLD_INGOT;
            default -> Items.ECHO_SHARD;
        };
        if (!feed.is(catalyst)) {
            setStatus(8);
            return;
        }
        Item result = switch (previous) {
            case 0 -> AEItems.CULTIVATED_FARMLAND.get();
            case 1 -> AEItems.NUTRIENT_FARMLAND.get();
            case 2 -> AEItems.MYTHIC_FARMLAND.get();
            case 3 -> AEItems.ARCANE_FARMLAND.get();
            default -> AEItems.CELESTIAL_FARMLAND.get();
        };
        // Refinement modules increase throughput, not resource efficiency.
        // Every resulting block costs exactly one farmland input and one catalyst.
        int batchLimit = data.get(0) == 1 ? 1 : data.get(0) == 2 ? 2 : 4;
        int batch = Math.min(batchLimit, Math.min(source.getCount(), feed.getCount()));
        if (batch <= 0) {
            setStatus(7);
            return;
        }
        if (!player.getAbilities().instabuild) {
            source.shrink(batch);
            feed.shrink(batch);
        }
        setItem(OUTPUT, new ItemStack(result, batch));
        lastBatch = batch;
        setChanged();
        setStatus(11);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider providers) {
        super.saveAdditional(tag, providers);
        ContainerHelper.saveAllItems(tag, items, providers);
        tag.putInt("Status", status);
        tag.putInt("LastBatch", lastBatch);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider providers) {
        super.loadAdditional(tag, providers);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, providers);
        status = Math.max(0, Math.min(16, tag.getInt("Status")));
        lastBatch = Math.max(0, Math.min(4, tag.getInt("LastBatch")));
    }
}
