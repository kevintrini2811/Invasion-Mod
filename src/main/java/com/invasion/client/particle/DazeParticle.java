package com.invasion.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class DazeParticle extends TextureSheetParticle {

    public static ParticleProvider<SimpleParticleType> factory(SpriteSet spriteProvider) {
        return (type, world, x, y, z, dX, dY, dZ) -> new DazeParticle(world, x, y, z, spriteProvider);
    }

    DazeParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider) {
        super(world, x, y, z);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        lifetime = 10;
        gravity = 0;
        setSpriteFromAge(spriteProvider);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

}
