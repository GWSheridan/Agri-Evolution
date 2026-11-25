package com.agrievolution.content;

import com.agrievolution.platform.ContentRegistration;
import com.agrievolution.platform.RegistrationSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public final class AEParticles {
    public static final RegistrationSet<net.minecraft.core.particles.ParticleType<?>> TYPES =
            new RegistrationSet<>(Registries.PARTICLE_TYPE);
    public static final java.util.function.Supplier<SimpleParticleType> SPRINKLER_WATER =
            TYPES.register("sprinkler_water", () -> ContentRegistration.platform().simpleParticle(false));
    private AEParticles() {}
}
