package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.BurrowerEntityModel;
import com.invasion.entity.BurrowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class BurrowerEntityRenderer extends LivingEntityRenderer<BurrowerEntity, BurrowerEntityModel> {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/burrower.png");

    public BurrowerEntityRenderer(Context ctx) {
        super(ctx, new BurrowerEntityModel(BurrowerEntityModel.getTexturedModelData2().bakeRoot()), 0.5F);
    }

    @Override
    protected void scale(BurrowerEntity entity, PoseStack matrices, float amount) {
        matrices.translate(0.0F, 0.45F, 0.0F);
        matrices.scale(2.2F, 2.2F, 2.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(BurrowerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(BurrowerEntity entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }
}
