package com.agrievolution.content;

import com.agrievolution.AgriEvolution;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AEParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, AgriEvolution.MOD_ID);
    public static final java.util.function.Supplier<SimpleParticleType> SPRINKLER_WATER =
            TYPES.register("sprinkler_water", () -> new SimpleParticleType(false));
    private AEParticles() {}
}
