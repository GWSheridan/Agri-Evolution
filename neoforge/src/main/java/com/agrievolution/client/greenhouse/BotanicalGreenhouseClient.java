package com.agrievolution.client.greenhouse;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.AEMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BotanicalGreenhouseClient {
    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AEMenus.BOTANICAL_GREENHOUSE.get(), BotanicalGreenhouseScreen::new);
        event.register(AEMenus.VERTICAL_GREENHOUSE.get(), VerticalGreenhouseScreen::new);
        event.register(AEMenus.BOTANICAL_GREENHOUSE_STORAGE.get(), BotanicalGreenhouseStorageScreen::new);
        event.register(AEMenus.BOTANICAL_GREENHOUSE_HARVEST.get(), BotanicalGreenhouseHarvestScreen::new);
        event.register(AEMenus.BOTANICAL_GREENHOUSE_SEED_BANK.get(), BotanicalGreenhouseSeedBankScreen::new);
    }
    private BotanicalGreenhouseClient() {}
}
