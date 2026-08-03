package com.invasion.client.render;

import com.invasion.entity.IMZoglinEntity;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class IMZoglinRenderer
        extends MobRenderer<IMZoglinEntity, HoglinModel<IMZoglinEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/hoglin/zoglin.png");

    public IMZoglinRenderer(EntityRendererProvider.Context context) {
        super(context, new HoglinModel<>(context.bakeLayer(ModelLayers.ZOGLIN)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(IMZoglinEntity entity) {
        return TEXTURE;
    }
}
