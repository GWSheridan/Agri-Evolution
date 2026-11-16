package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** One authoritative tank at the former centre source; shell pieces never hold water. */
public final class WaterReservoirBlockEntity extends BlockEntity implements IFluidHandler {
    public static final int CAPACITY=32000;
    private int water;
    private boolean renewable;
    private int generationSeconds;
    public boolean renewable(){return renewable;}
    public boolean upgrade(){if(renewable||!complete())return false;renewable=true;generationSeconds=0;setChanged();return true;}
    boolean removeUpgrade(){boolean result=renewable;renewable=false;setChanged();return result;}
    public void generationSecond(){
        if (complete() && !level.isClientSide) generateSecondInCompleteTank();
    }
    void generateSecondInCompleteTank() {
        if (!renewable || water == CAPACITY) return;
        if (++generationSeconds >= 10) {
            generationSeconds = 0;
            setWater(Math.min(CAPACITY, water + 1000));
        } else setChanged();
    }
    public WaterReservoirBlockEntity(BlockPos pos,BlockState state) {super(AEBlockEntities.WATER_RESERVOIR.get(),pos,state);}
    public int water(){return water;}
    public int comparatorSignal() { return water == 0 ? 0 : 1 + 14 * water / CAPACITY; }
    private static int gaugeLevel(int amount) { return (amount + 7999) / 8000; }
    public boolean complete() {
        return !isRemoved() && level != null && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && ReservoirStructure.complete(level, worldPosition);
    }
    @Override public int getTanks(){return 1;}
    @Override public FluidStack getFluidInTank(int tank) { return tank == 0 && water > 0 && complete() ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank){return tank==0?CAPACITY:0;}
    @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tank == 0 && fluid.is(Fluids.WATER); }
    @Override public int fill(FluidStack fluid,FluidAction action){
        if(!complete()||level.isClientSide||!isFluidValid(0,fluid))return 0;
        int amount=Math.min(CAPACITY-water,fluid.getAmount());
        if (action.execute() && amount > 0) setWater(water + amount);
        return amount;
    }
    @Override public FluidStack drain(FluidStack fluid, FluidAction action) { return isFluidValid(0, fluid) ? drain(fluid.getAmount(), action) : FluidStack.EMPTY; }
    @Override public FluidStack drain(int maximum,FluidAction action){
        if (maximum <= 0 || water == 0 || !complete() || level.isClientSide) return FluidStack.EMPTY;
        int amount = Math.min(water, maximum);
        if (action.execute()) setWater(water - amount);
        return new FluidStack(Fluids.WATER, amount);
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
        setChanged();
        int fill = gaugeLevel(water);
        boolean updateGauge = gaugeLevel(previous) != fill;
        if (level == null) return;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var p=worldPosition.offset(x,y,z);if(!level.hasChunkAt(p))continue;var state=level.getBlockState(p);
            if (state.getBlock() instanceof WaterReservoirBlock && WaterReservoirBlock.core(p, state).equals(worldPosition)) {
                if (updateGauge && state.getValue(WaterReservoirBlock.FILL) != fill) {
                    level.setBlock(p, state.setValue(WaterReservoirBlock.FILL, fill),
                            IrrigationNetwork.loadedUpdateFlags(level, p, Block.UPDATE_CLIENTS));
                }
                IrrigationNetwork.updateComparators(level, p, state.getBlock());
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.saveAdditional(tag,registries);tag.putInt("Water",water);tag.putBoolean("Renewable",renewable);tag.putInt("GenerationSeconds",generationSeconds);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.loadAdditional(tag,registries);water=Math.clamp(tag.getInt("Water"),0,CAPACITY);renewable=tag.getBoolean("Renewable");generationSeconds=Math.clamp(tag.getInt("GenerationSeconds"),0,9);}
}
