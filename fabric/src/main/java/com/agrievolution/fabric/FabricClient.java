package com.agrievolution.fabric;

import com.agrievolution.client.*;
import com.agrievolution.client.greenhouse.GreenhouseHologramClient;
import com.agrievolution.client.journal.MilestonePage;
import com.agrievolution.content.*;
import com.agrievolution.platform.ModInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.item.ItemProperties;

public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientMenus.register(MenuScreens::register);
        var cutout = net.minecraft.client.renderer.RenderType.cutout();
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block instanceof AgriCropBlock) BlockRenderLayerMap.INSTANCE.putBlock(block, cutout);
        }
        BlockRenderLayerMap.INSTANCE.putBlocks(cutout, AEBlocks.IRRIGATION_BASIN.get(),
                AEBlocks.VERTICAL_GREENHOUSE.get(), AEBlocks.CULTIVATED_FARMLAND.get(),
                AEBlocks.NUTRIENT_FARMLAND.get(), AEBlocks.MYTHIC_FARMLAND.get(), AEBlocks.ARCANE_FARMLAND.get(),
                AEBlocks.CELESTIAL_FARMLAND.get());
        EntityRendererRegistry.register(AEEntities.CROP_WEED.get(), CropWeedRenderer::new);
        ParticleFactoryRegistry.getInstance().register(AEParticles.SPRINKLER_WATER.get(), SprinklerWaterParticle::provider);
        ColorProviderRegistry.BLOCK.register((state, level, pos, index) -> index == 0
                ? level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4 : -1,
                AEBlocks.IRRIGATION_BASIN.get());
        ItemProperties.register(AEItems.SEED_BAG.get(), ModInfo.id("fill"),
                (stack, level, entity, seed) -> SeedBagItem.fillState(stack));
        ItemProperties.register(AEItems.TROWEL.get(), ModInfo.id("tended"),
                (stack, level, entity, seed) -> GardenToolItem.recentlyUsed(stack, level) ? 1 : 0);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FarmLedgerClient.tick();
            GreenhouseHologramClient.onClientTick();
        });
        HudRenderCallback.EVENT.register((gui, delta) -> {
            FarmInspectionClient.hud(gui);
            FarmLedgerClient.hud(gui);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var pose = context.matrixStack();
            if (pose == null) return;
            var camera = context.camera().getPosition();
            FarmInspectionClient.preview(pose, camera);
            FarmLedgerClient.render(pose, camera);
            GreenhouseHologramClient.onRenderLevel(pose, camera);
        });
        ClientPlayNetworking.registerGlobalReceiver(FarmLedgerPayload.Response.TYPE, (p, c) -> FarmLedgerClient.receive(p));
        ClientPlayNetworking.registerGlobalReceiver(InspectionPayload.TYPE, (p, c) -> FarmInspectionClient.receive(p));
        ClientPlayNetworking.registerGlobalReceiver(FarmingMilestonesPayload.TYPE, (p, c) -> MilestonePage.receive(p.mask()));
        ClientPlayNetworking.registerGlobalReceiver(IrrigationZoneLabelsPayload.TYPE, (p, c) -> p.apply(c.player()));
    }
}
