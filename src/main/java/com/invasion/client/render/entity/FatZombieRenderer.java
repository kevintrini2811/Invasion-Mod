package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.IMFatZombieEntity;
import com.invasion.client.render.entity.model.FatZombieModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;

public final class FatZombieRenderer extends MobRenderer<IMFatZombieEntity, FatZombieModel> {
    private static final ResourceLocation TEXTURE =
            InvasionMod.id("textures/entity/fat_zombie.png");

    public FatZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new FatZombieModel(
                FatZombieModel.createBodyLayer().bakeRoot()), 0.8F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(IMFatZombieEntity entity, PoseStack poseStack, float partialTick) {
        float scale = entity.getGrowthScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(IMFatZombieEntity entity) {
        return TEXTURE;
    }
}
