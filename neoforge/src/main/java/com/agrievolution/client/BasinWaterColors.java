package com.agrievolution.client;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.AEBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Vanilla animated water sprite with cauldron-style biome colour; timber remains untinted. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BasinWaterColors {
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event){
        event.register((state,level,pos,index)->index==0
            ? level!=null&&pos!=null?BiomeColors.getAverageWaterColor(level,pos):0x3F76E4
            : -1,AEBlocks.IRRIGATION_BASIN.get());
    }
    private BasinWaterColors(){}
}
