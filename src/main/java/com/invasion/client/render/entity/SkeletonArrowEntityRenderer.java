package com.invasion.client.render.entity;

import com.invasion.entity.SkeletonArrowEntity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class SkeletonArrowEntityRenderer
        extends ArrowRenderer<SkeletonArrowEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/projectiles/arrow.png");

    public SkeletonArrowEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonArrowEntity entity) {
        return TEXTURE;
    }
}
