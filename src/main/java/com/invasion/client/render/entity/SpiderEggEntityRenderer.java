package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.EggModel;
import com.invasion.entity.SpiderEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class SpiderEggEntityRenderer extends EntityRenderer<SpiderEggEntity> {
	private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/spider_egg.png");

	private final EggModel model = new EggModel(EggModel.getTexturedModelData().bakeRoot());

	public SpiderEggEntityRenderer(EntityRendererProvider.Context context) {
	    super(context);
	}

	@Override
    public void render(SpiderEggEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
	    model.renderToBuffer(matrices, vertexConsumers.getBuffer(model.renderType(getTextureLocation(entity))), light, 0);
	}

    @Override
    public ResourceLocation getTextureLocation(SpiderEggEntity entity) {
        return TEXTURE;
    }
}