package com.invasion.client.render;

import com.invasion.entity.SkeletonArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.ResourceLocation;

public class SkeletonArrowRenderer extends ArrowRenderer<SkeletonArrowEntity, ArrowRenderState> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/projectiles/arrow.png");

    public SkeletonArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ResourceLocation getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
