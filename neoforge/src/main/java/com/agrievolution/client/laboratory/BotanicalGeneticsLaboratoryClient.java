package com.agrievolution.client.laboratory;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.AEMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BotanicalGeneticsLaboratoryClient {
    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AEMenus.BOTANICAL_GENETICS_LABORATORY.get(), BotanicalGeneticsLaboratoryScreen::new);
    }
    private BotanicalGeneticsLaboratoryClient() { }
}
