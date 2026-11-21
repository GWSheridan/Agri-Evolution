package com.agrievolution.client;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AgriEvolution.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GardenToolsClient {
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AEEntities.CROP_WEED.get(), CropWeedRenderer::new);
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(AEMenus.FARM_LEDGER.get(), FarmLedgerScreen::new);
        event.register(AEMenus.SEED_BAG.get(), SeedBagScreen::new);
        event.register(AEMenus.LUNCH_BOX.get(), LunchBoxScreen::new);
        event.register(AEMenus.GARDEN_WORK.get(), GardenWorkScreen::new);
        event.register(AEMenus.SEED_VAULT.get(), SeedVaultScreen::new);
        event.register(AEMenus.IRRIGATION_CONTROLLER.get(), IrrigationControllerScreen::new);
        event.register(AEMenus.BATCH_COOKING_STATION.get(), BatchCookingScreen::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(AEItems.SEED_BAG.get(), ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, "fill"),
                    (stack, level, entity, seed) -> SeedBagItem.fillState(stack));
            ItemProperties.register(AEItems.TROWEL.get(), ResourceLocation.fromNamespaceAndPath(AgriEvolution.MOD_ID, "tended"),
                    (stack, level, entity, seed) -> GardenToolItem.recentlyUsed(stack, level) ? 1 : 0);
        });
    }
    private GardenToolsClient() {}
}
