package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.resources.ResourceLocation;

public final class IMWolfRenderer extends WolfRenderer {
    private static final ResourceLocation TEXTURE =
            InvasionMod.id("textures/entity/wolf/tame_nexus.png");

    public IMWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(
            Wolf wolf, WolfRenderState state, float partialTick) {
        super.extractRenderState(wolf, state, partialTick);
        state.bodyArmorItem = wolf.getBodyArmorItem().copy();
    }

    @Override
    protected void scale(WolfRenderState state, PoseStack poseStack) {
        float scale = 1.3F;
        poseStack.scale(scale, (2.0F + scale) / 3.0F, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(WolfRenderState state) {
        return TEXTURE;
    }
}
