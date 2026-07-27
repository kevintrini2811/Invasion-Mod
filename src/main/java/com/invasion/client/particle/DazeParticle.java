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

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return SingleQuadParticle.FacingCameraMode.LOOKAT_Y;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float tickDelta) {
        super.render(buffer, camera, tickDelta);
    }

    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z, float tickDelta) {
        for (int i = 0; i < 6; i++) {
            float time = (age + (i * 10) + tickDelta) / 10F;
            float dX = Mth.sin(time) * 0.5F;
            float dZ = Mth.cos(time) * 0.5F;
            float dY = Mth.sin(time * 2) * 0.1F;

            super.renderRotatedQuad(buffer, rotation, x + dX, y + dY, z + dZ, tickDelta);
        }
    }

}
