package com.agrievolution.forge;

import com.agrievolution.client.*;
import com.agrievolution.client.greenhouse.GreenhouseHologramClient;
import com.agrievolution.client.journal.MilestonePage;
import com.agrievolution.content.*;
import com.agrievolution.platform.ModInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ModInfo.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClient {
    private ForgeClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientMenus.register(MenuScreens::register);
            ItemProperties.register(AEItems.SEED_BAG.get(), ModInfo.id("fill"),
                    (stack, level, entity, seed) -> SeedBagItem.fillState(stack));
            ItemProperties.register(AEItems.TROWEL.get(), ModInfo.id("tended"),
                    (stack, level, entity, seed) -> GardenToolItem.recentlyUsed(stack, level) ? 1 : 0);
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post tick) -> {
            FarmLedgerClient.tick();
            GreenhouseHologramClient.onClientTick();
        });
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::renderLevel);
    }

    @SubscribeEvent
    public static void hud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(ModInfo.id("farm_overlay"),
                net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.HOTBAR, (gui, delta) -> {
                    FarmInspectionClient.hud(gui);
                    FarmLedgerClient.hud(gui);
                });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AEEntities.CROP_WEED.get(), CropWeedRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AEParticles.SPRINKLER_WATER.get(), SprinklerWaterParticle::provider);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, index) -> index == 0
                ? level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4 : -1,
                AEBlocks.IRRIGATION_BASIN.get());
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var pose = new PoseStack();
        pose.mulPose(event.getPoseStack());
        var camera = event.getCamera().getPosition();
        FarmInspectionClient.preview(pose, camera);
        FarmLedgerClient.render(pose, camera);
        GreenhouseHologramClient.onRenderLevel(pose, camera);
    }

    static void receive(FarmLedgerPayload.Response payload) { FarmLedgerClient.receive(payload); }
    static void receive(FarmingMilestonesPayload payload) { MilestonePage.receive(payload.mask()); }
    static void receive(InspectionPayload payload) { FarmInspectionClient.receive(payload); }
    static void receive(IrrigationZoneLabelsPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null) payload.apply(player);
    }
}
