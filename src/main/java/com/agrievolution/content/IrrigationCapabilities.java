package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.*;

/** Only the six outward face-centre ports expose the authoritative core tank. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class IrrigationCapabilities {
    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event){
        event.registerBlock(Capabilities.FluidHandler.BLOCK,(level,pos,state,entity,side)->{
            if(side==null||!WaterReservoirBlock.port(state,side))return null;
            var core=WaterReservoirBlock.core(pos,state);
            return level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof WaterReservoirBlockEntity tank?tank:null;
        },AEBlocks.WATER_RESERVOIR.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, side) ->
                VerticalGreenhouseBlock.port(state, side)
                        && entity instanceof VerticalGreenhouseBlockEntity greenhouse ? greenhouse : null, AEBlocks.VERTICAL_GREENHOUSE.get());
    }
}
