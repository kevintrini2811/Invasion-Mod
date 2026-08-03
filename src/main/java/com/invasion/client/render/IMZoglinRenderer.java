package com.invasion.client.render;

import com.invasion.entity.IMZoglinEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHoglinRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.resources.Identifier;

public final class IMZoglinRenderer extends AbstractHoglinRenderer<IMZoglinEntity> {
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/hoglin/zoglin.png");

    public IMZoglinRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.ZOGLIN, ModelLayers.ZOGLIN_BABY, 0.7F);
    }

    @Override
    public Identifier getTextureLocation(HoglinRenderState state) {
        return TEXTURE;
    }
}
