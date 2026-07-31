package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class IMWolfEntityRenderer extends WolfRenderer {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/wolf/tame_nexus.png");

	public IMWolfEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
	}

    @Override
    protected void scale(Wolf entity, PoseStack matrices, float amount) {
        float f = 1.3F;
        matrices.scale(f, (2 + f) / 3F, f);
    }

	@Override
	public ResourceLocation getTextureLocation(Wolf entity) {
	    // TODO: Wolves have variant textures now
		return TEXTURE;
	}
}