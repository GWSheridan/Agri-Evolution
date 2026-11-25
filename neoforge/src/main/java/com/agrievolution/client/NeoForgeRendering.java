package com.agrievolution.client;

import com.agrievolution.client.greenhouse.GreenhouseHologramClient;
import com.agrievolution.content.AEParticles;
import com.agrievolution.platform.ModInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ModInfo.ID, value = Dist.CLIENT)
public final class NeoForgeRendering {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        FarmLedgerClient.tick();
        GreenhouseHologramClient.onClientTick();
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        FarmInspectionClient.hud(event.getGuiGraphics());
        FarmLedgerClient.hud(event.getGuiGraphics());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        FarmInspectionClient.preview(pose, camera);
        FarmLedgerClient.render(pose, camera);
        GreenhouseHologramClient.onRenderLevel(pose, camera);
    }

    @EventBusSubscriber(modid = ModInfo.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Particles {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(AEParticles.SPRINKLER_WATER.get(), SprinklerWaterParticle::provider);
        }
    }
}
