package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Only the six outward face-centre ports expose the authoritative core tank. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class IrrigationCapabilities {
    private static final WeakHashMap<WaterTank, WeakReference<IFluidHandler>> HANDLERS = new WeakHashMap<>();

    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event){
        event.registerBlock(Capabilities.FluidHandler.BLOCK,(level,pos,state,entity,side)->{
            if(side==null||!WaterReservoirBlock.port(state,side))return null;
            var core=WaterReservoirBlock.core(pos,state);
            return level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank?handler(tank):null;
        },AEBlocks.WATER_RESERVOIR.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) ->
                VerticalGreenhouseBlock.port(state, side)
                        && entity instanceof VerticalGreenhouseBlockEntity greenhouse ? handler(greenhouse) : null, AEBlocks.VERTICAL_GREENHOUSE.get());
    }

    private static synchronized IFluidHandler handler(WaterTank tank) {
        var reference = HANDLERS.get(tank);
        var current = reference == null ? null : reference.get();
        if (current != null) return current;
        var handler = new TankHandler(tank);
        HANDLERS.put(tank, new WeakReference<>(handler));
        return handler;
    }

    private record TankHandler(WaterTank tank) implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int index) {
            return index == 0 && tank.complete() ? asStack(tank.water()) : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int index) { return index == 0 ? tank.capacity() : 0; }
        @Override public boolean isFluidValid(int index, FluidStack stack) {
            return index == 0 && stack.is(Fluids.WATER);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? tank.fillWater(stack.getAmount(), action.execute()) : 0;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return asStack(tank.drainWater(amount, action.execute()));
        }
        private static FluidStack asStack(int amount) {
            return amount > 0 ? new FluidStack(Fluids.WATER, amount) : FluidStack.EMPTY;
        }
    }
}
