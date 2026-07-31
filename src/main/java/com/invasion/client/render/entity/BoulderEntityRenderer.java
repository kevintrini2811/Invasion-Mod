package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.BoulderEntityModel;
import com.invasion.entity.BoulderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BoulderEntityRenderer extends EntityRenderer<BoulderEntity> {
	private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/boulder.png");

    private final BoulderEntityModel model = new BoulderEntityModel(BoulderEntityModel.getTexturedModelData().bakeRoot());

    public BoulderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(BoulderEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        model.renderToBuffer(matrices, vertexConsumers.getBuffer(model.renderType(getTextureLocation(entity))), light, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(BoulderEntity entity) {
        return TEXTURE;
    }
}