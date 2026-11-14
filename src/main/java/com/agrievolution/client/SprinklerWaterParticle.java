package com.agrievolution.client;

import com.agrievolution.AgriEvolution;
import com.agrievolution.content.AEParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Small translucent droplets with real launch velocity, gravity and collision. */
@EventBusSubscriber(modid=AgriEvolution.MOD_ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class SprinklerWaterParticle extends TextureSheetParticle {
    private SprinklerWaterParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        xd=vx; yd=vy; zd=vz;
        quadSize=.018f+random.nextFloat()*.012f;
        setSize(.02f,.02f);
        gravity=.12f;
        friction=.98f;
        lifetime=30+random.nextInt(10);
        setColor(.45f,.78f,1f);
        setAlpha(.72f);
        pickSprite(sprites);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override public void tick() {
        super.tick();
        if (onGround) remove();
        else if (age>lifetime-8) setAlpha(.72f*(lifetime-age)/8f);
    }
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AEParticles.SPRINKLER_WATER.get(), sprites ->
                (type, level, x, y, z, vx, vy, vz) -> new SprinklerWaterParticle(level,x,y,z,vx,vy,vz,sprites));
    }
}
