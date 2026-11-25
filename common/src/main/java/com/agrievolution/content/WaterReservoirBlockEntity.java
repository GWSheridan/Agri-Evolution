package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One authoritative tank at the former centre source; shell pieces never hold water. */
public final class WaterReservoirBlockEntity extends BlockEntity implements WaterTank {
    public static final int CAPACITY=32000;
    private int water;
    private boolean renewable;
    private int generationSeconds;
    public boolean renewable(){return renewable;}
    public boolean upgrade(){if(renewable||!complete())return false;renewable=true;generationSeconds=0;setChanged();return true;}
    boolean removeUpgrade(){boolean result=renewable;renewable=false;setChanged();return result;}
    public void generationSecond(){
        generateSecondInCompleteTank();
    }
    void generateSecondInCompleteTank() {
        if (!complete() || level.isClientSide || !renewable || water == CAPACITY) return;
        if (++generationSeconds >= 10) {
            generationSeconds = 0;
            setWater(Math.min(CAPACITY, water + 1000));
        } else setChanged();
    }
    public WaterReservoirBlockEntity(BlockPos pos,BlockState state) {super(AEBlockEntities.WATER_RESERVOIR.get(),pos,state);}
    public int water(){return water;}
    public int comparatorSignal() { return water == 0 ? 0 : 1 + 14 * water / CAPACITY; }
    static int gaugeLevel(int amount) { return (amount + 7999) / 8000; }
    public boolean complete() {
        return !isRemoved() && level != null && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && ReservoirStructure.complete(level, worldPosition);
    }
    @Override public int capacity() { return CAPACITY; }
    @Override public int fillWater(int maximum, boolean execute) {
        if (maximum <= 0 || !complete() || level.isClientSide) return 0;
        int amount = Math.min(CAPACITY - water, maximum);
        if (execute && amount > 0) setWater(water + amount);
        return amount;
    }
    @Override public int drainWater(int maximum, boolean execute) {
        if (maximum <= 0 || water == 0 || !complete() || level.isClientSide) return 0;
        int amount = Math.min(water, maximum);
        if (execute) setWater(water - amount);
        return amount;
    }
    public boolean addBucket(){if(!complete()||water>CAPACITY-1000)return false;setWater(water+1000);return true;}
    public boolean takeBucket(){if(!complete()||water<1000)return false;setWater(water-1000);return true;}
    void initialize(){setWater(1000);}

    @Override public void setChanged() {
        // The core only owns storage. Water changes notify comparators at the shell below.
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    private void setWater(int amount) {
        int previous = water;
        water = amount;
        if (water == CAPACITY) generationSeconds = 0;
        setChanged();
        int fill = gaugeLevel(water);
        boolean updateGauge = gaugeLevel(previous) != fill;
        if (level == null) return;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var p=worldPosition.offset(x,y,z);if(!level.hasChunkAt(p))continue;var state=level.getBlockState(p);
            if (state.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.core(p, state).equals(worldPosition)) {
                if (updateGauge) WaterReservoirBlock.refreshGauge(level, p, state, fill);
                IrrigationNetwork.updateComparators(level, p, state.getBlock());
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.saveAdditional(tag,registries);tag.putInt("Water",water);tag.putBoolean("Renewable",renewable);tag.putInt("GenerationSeconds",generationSeconds);}
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = Math.clamp(tag.getInt("Water"), 0, CAPACITY);
        renewable = tag.getBoolean("Renewable");
        generationSeconds = water == CAPACITY ? 0 : Math.clamp(tag.getInt("GenerationSeconds"), 0, 9);
    }
}
