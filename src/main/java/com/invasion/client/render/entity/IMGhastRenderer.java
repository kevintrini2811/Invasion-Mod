package com.invasion.client.render.entity;

import com.invasion.entity.IMGhastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.GhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class IMGhastRenderer
        extends MobRenderer<IMGhastEntity, GhastModel<IMGhastEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast.png");
    private static final ResourceLocation SHOOTING_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

    public IMGhastRenderer(EntityRendererProvider.Context context) {
        super(context, new GhastModel<>(context.bakeLayer(ModelLayers.GHAST)), 1.5F);
        addLayer(new MobHeadArmorLayer<>(this, context,
                ghast -> getModel().root().getChild("body"),
                2.268F, -1.30F, 0.0F));
    }

    @Override
    protected void scale(
            IMGhastEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(4.5F, 4.5F, 4.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(IMGhastEntity entity) {
        return entity.isCharging() ? SHOOTING_TEXTURE : TEXTURE;
    }
}
