package com.agrievolution.client.soil;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.AEMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SoilResearchTableClient {
    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(AEMenus.SOIL_RESEARCH_TABLE.get(), SoilResearchTableScreen::new);
    }
    private SoilResearchTableClient() {}
}
