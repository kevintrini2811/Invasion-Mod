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
        matrices.scale(2.2F, 2.2F, 2.2F);
        // Cancel LivingEntityRenderer's humanoid model-origin offset. Every
        // burrower segment already carries its position relative to the head.
        matrices.translate(0.0F, 1.501F, 0.0F);
    }

    @Override
    protected void setupRotations(BurrowerEntity entity, PoseStack matrices,
            float bob, float bodyYaw, float partialTick) {
        // Segment rotations are complete world-space rotations; applying the
        // normal mob body yaw would rotate the entire chain a second time.
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
