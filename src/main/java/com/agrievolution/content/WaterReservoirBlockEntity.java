package com.agrievolution.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One authoritative tank at the former centre source; shell pieces never hold water. */
public final class WaterReservoirBlockEntity extends BlockEntity implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
    public static final int CAPACITY=32000;
    private int water;
    private boolean renewable;
    private int generationSeconds;
    public boolean renewable(){return renewable;}
    public boolean upgrade(){if(renewable||!complete())return false;renewable=true;generationSeconds=0;setChanged();return true;}
    boolean removeUpgrade(){boolean result=renewable;renewable=false;setChanged();return result;}
    public void generationSecond(){
        if(level==null||level.isClientSide||!renewable||!complete()||water==CAPACITY)return;
        if(++generationSeconds>=10){generationSeconds=0;water=Math.min(CAPACITY,water+1000);changed();}
        else setChanged();
    }
    public WaterReservoirBlockEntity(BlockPos pos,BlockState state) {super(AEBlockEntities.WATER_RESERVOIR.get(),pos,state);}
    public int water(){return water;}
    public int comparatorSignal(){return water==0?0:1+14*water/CAPACITY;}
    public boolean complete(){return !isRemoved() && level!=null && level.getBlockEntity(worldPosition)==this && ReservoirStructure.complete(level,worldPosition);}
    @Override public int getTanks(){return 1;}
    @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank){return tank==0&&complete()?new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,water):net.neoforged.neoforge.fluids.FluidStack.EMPTY;}
    @Override public int getTankCapacity(int tank){return tank==0?CAPACITY:0;}
    @Override public boolean isFluidValid(int tank,net.neoforged.neoforge.fluids.FluidStack fluid){return tank==0&&fluid.is(net.minecraft.world.level.material.Fluids.WATER);}
    @Override public int fill(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){
        if(!complete()||level.isClientSide||!isFluidValid(0,fluid))return 0;
        int amount=Math.min(CAPACITY-water,fluid.getAmount());
        if(action.execute()&&amount>0){water+=amount;changed();}return amount;
    }
    @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){return isFluidValid(0,fluid)?drain(fluid.getAmount(),action):net.neoforged.neoforge.fluids.FluidStack.EMPTY;}
    @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maximum,FluidAction action){
        if(!complete()||level.isClientSide||maximum<=0)return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        int amount=Math.min(water,maximum);if(action.execute()&&amount>0){water-=amount;changed();}
        return new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,amount);
    }
    public boolean addBucket(){if(!complete()||water>CAPACITY-1000)return false;water+=1000;changed();return true;}
    public boolean takeBucket(){if(!complete()||water<1000)return false;water-=1000;changed();return true;}
    void initialize(){water=1000;changed();}
    private void changed(){
        setChanged();if(level==null)return;
        int fill=water==0?0:Math.min(4,(water+7999)/8000);
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var p=worldPosition.offset(x,y,z);if(!level.hasChunkAt(p))continue;var state=level.getBlockState(p);
            if(state.getBlock() instanceof WaterReservoirBlock&&WaterReservoirBlock.core(p,state).equals(worldPosition)
                    ){
                if(state.getValue(WaterReservoirBlock.FILL)!=fill)level.setBlock(p,state.setValue(WaterReservoirBlock.FILL,fill),2);
                level.updateNeighbourForOutputSignal(p,state.getBlock());
                level.invalidateCapabilities(p);
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.saveAdditional(tag,registries);tag.putInt("Water",water);tag.putBoolean("Renewable",renewable);tag.putInt("GenerationSeconds",generationSeconds);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.loadAdditional(tag,registries);water=Math.clamp(tag.getInt("Water"),0,CAPACITY);renewable=tag.getBoolean("Renewable");generationSeconds=Math.clamp(tag.getInt("GenerationSeconds"),0,9);}
}
