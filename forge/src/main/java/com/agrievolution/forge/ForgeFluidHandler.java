package com.agrievolution.forge;

import com.agrievolution.content.VerticalGreenhouseBlock;
import com.agrievolution.content.VerticalGreenhouseBlockEntity;
import com.agrievolution.content.WaterTank;
import com.agrievolution.platform.ModInfo;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

record ForgeFluidHandler(Supplier<WaterTank> source) implements IFluidHandler {
    static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof VerticalGreenhouseBlockEntity greenhouse)) return;
        var fluid = LazyOptional.<IFluidHandler>of(() -> new ForgeFluidHandler(
                () -> greenhouse.isRemoved() ? null : greenhouse));
        event.addCapability(ModInfo.id("greenhouse_water"), new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction face) {
                if (cap == ForgeCapabilities.FLUID_HANDLER && !greenhouse.isRemoved()
                        && VerticalGreenhouseBlock.port(greenhouse.getBlockState(), face)) return fluid.cast();
                return LazyOptional.empty();
            }
        });
        event.addListener(fluid::invalidate);
    }

    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int index) {
        var tank = source.get();
        return index == 0 && tank != null && tank.complete() ? asStack(tank.water()) : FluidStack.EMPTY;
    }
    @Override public int getTankCapacity(int index) {
        var tank = source.get();
        return index == 0 && tank != null ? tank.capacity() : 0;
    }
    @Override public boolean isFluidValid(int index, FluidStack stack) {
        return index == 0 && stack.getFluid() == Fluids.WATER;
    }
    @Override public int fill(FluidStack stack, FluidAction action) {
        var tank = source.get();
        return tank != null && isFluidValid(0, stack) ? tank.fillWater(stack.getAmount(), action.execute()) : 0;
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        return isFluidValid(0, stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        var tank = source.get();
        return tank == null ? FluidStack.EMPTY : asStack(tank.drainWater(amount, action.execute()));
    }

    private static FluidStack asStack(int amount) {
        return amount > 0 ? new FluidStack(Fluids.WATER, amount) : FluidStack.EMPTY;
    }
}
