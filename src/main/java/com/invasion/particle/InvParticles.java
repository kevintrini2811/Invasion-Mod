package com.invasion.particle;

import com.invasion.InvasionMod;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public interface InvParticles {
    SimpleParticleType DAZE = register("daze", new SimpleParticleType(true));

    static <T extends ParticleType<?>> T register(String name, T type) {
        return InvasionMod.INSTANCE.register(
                net.minecraft.core.registries.Registries.PARTICLE_TYPE, InvasionMod.id(name), type);
    }

    static void bootstrap() {}
}
