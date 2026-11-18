package com.agrievolution.content;

import com.agrievolution.genetics.CropGenome;
import com.agrievolution.genetics.GenomeStorage;
import com.agrievolution.genetics.HybridLineageStorage;
import com.agrievolution.genetics.RareTrait;
import com.agrievolution.genetics.SeedVariety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A single planted crop's persistent genotype and one-cross-per-lifecycle flag. */
public final class AECropBlockEntity extends BlockEntity {
    private static final String GENOME = "AgriGenome";
    private static final String CROSSED = "AgriCrossed";
    private static final String NUTRITION = "AgriSoilNutrition";

    private CropGenome genome = CropGenome.wild();
    private RareTrait rareTrait = RareTrait.NONE;
    private HybridLineageStorage.Lineage lineage;
    private String variety = "";
    private boolean crossed;
    // One non-destructive genetic clipping per planting; resets with the replanted block entity.
    private boolean clipped;
    private boolean tended;
    private long pollinatedUntil;
    private int soilNutrition = 40;
    private int supportTicks=-1;
    public int supportTicks(){return supportTicks<0?CropSupportItem.lifetime(CropSupportItem.tier(getBlockState())):supportTicks;}
    public void setSupportTicks(int ticks){supportTicks=ticks;setChanged();}
    public ItemStack recoverSupport() {
        int tier = CropSupportItem.tier(getBlockState());
        int remaining = supportTicks();
        if (tier == 0 || remaining == 0) return ItemStack.EMPTY;
        var stack = new ItemStack(CropSupportItem.itemFor(tier));
        if (tier < 3 && remaining < CropSupportItem.lifetime(tier)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("SupportTicks", remaining));
        }
        return stack;
    }
    public static void tickSupport(Level level, BlockPos pos, BlockState state, AECropBlockEntity plant) {
        int tier = CropSupportItem.tier(state);
        if (tier == 0) return;
        if (tier == 3) {
            if (state.getValue(AgriCropBlock.WEEDY)) {
                level.setBlock(pos, state.setValue(AgriCropBlock.WEEDY, false), Block.UPDATE_ALL);
            }
            return;
        }
        if (plant.supportTicks < 0) plant.supportTicks = CropSupportItem.lifetime(tier);
        if (plant.supportTicks > 0) {
            plant.supportTicks--;
            // The ticker's chunk is already loaded. Persist wear without updating neighboring comparators.
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
        if (plant.supportTicks == 0 && level.setBlock(pos, state.setValue(AgriCropBlock.SUPPORTED, false)
                .setValue(AgriCropBlock.SUPPORT_TIER, 0), Block.UPDATE_ALL)) {
            plant.setSupportTicks(-1);
        }
    }

    public AECropBlockEntity(BlockPos pos, BlockState state) {
        super(AEBlockEntities.AGRI_CROP.get(), pos, state);
    }

    public CropGenome genome() { return genome; }
    public RareTrait rareTrait() { return rareTrait; }
    public HybridLineageStorage.Lineage lineage() { return lineage; }
    public String variety() { return variety; }
    public void setVariety(String name) {
        variety = SeedVariety.clean(name);
        setChanged();
    }
    public void setLineage(HybridLineageStorage.Lineage lineage) {
        this.lineage = lineage;
        setChanged();
    }
    public void setRareTrait(RareTrait trait) {
        rareTrait = trait;
        setChanged();
    }
    public boolean hasCrossed() { return crossed; }
    public boolean hasBeenClipped() { return clipped; }
    public boolean hasBeenTended() { return tended; }
    boolean pollinated(long gameTime) { return pollinatedUntil > gameTime; }
    void markPollinated(long until) { pollinatedUntil = until; setChanged(); }
    public void setTended() { tended = true; setChanged(); }
    public void setClipped(boolean clipped) { this.clipped = clipped; setChanged(); }
    public int soilNutrition() { return soilNutrition; }

    public void addNutrition(int amount) {
        soilNutrition = com.agrievolution.farming.FarmEnvironment.clampNutrition(soilNutrition + amount);
        setChanged();
    }

    public void setGenome(CropGenome genome) {
        this.genome = genome;
        setChanged();
    }

    public void setCrossed(boolean value) {
        this.crossed = value;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(GENOME, GenomeStorage.writeGenome(genome));
        tag.putString("AgriRareTrait", rareTrait.name());
        HybridLineageStorage.write(tag, lineage);
        if (!variety.isEmpty()) tag.putString("AgriVariety", variety);
        tag.putBoolean(CROSSED, crossed);
        tag.putBoolean("AgriGeneticClipped", clipped);
        tag.putBoolean("AgriTrowelTended", tended);
        if (pollinatedUntil > 0) tag.putLong("PollinatedUntil", pollinatedUntil);
        tag.putInt(NUTRITION, soilNutrition);
        tag.putInt("SupportTicks",supportTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        supportTicks=tag.contains("SupportTicks")?Math.max(-1,tag.getInt("SupportTicks")):-1;
        genome = tag.contains(GENOME, 10)
                ? GenomeStorage.readGenome(tag.getCompound(GENOME)) : CropGenome.wild();
        rareTrait = RareTrait.fromName(tag.getString("AgriRareTrait"));
        lineage = HybridLineageStorage.read(tag);
        variety = SeedVariety.clean(tag.getString("AgriVariety"));
        crossed = tag.getBoolean(CROSSED);
        clipped = tag.getBoolean("AgriGeneticClipped");
        tended = tag.getBoolean("AgriTrowelTended");
        pollinatedUntil = tag.getLong("PollinatedUntil");
        soilNutrition = tag.contains(NUTRITION)
                ? com.agrievolution.farming.FarmEnvironment.clampNutrition(tag.getInt(NUTRITION)) : 40;
    }
}
