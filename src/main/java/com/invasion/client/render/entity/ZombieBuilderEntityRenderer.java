package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.ZombieBuilderEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public final class ZombieBuilderEntityRenderer extends HumanoidMobRenderer<ZombieBuilderEntity, HumanoidModel<ZombieBuilderEntity>> {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/zombie_builder.png");

    public ZombieBuilderEntityRenderer(Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieBuilderEntity entity) {
        return TEXTURE;
    }
}
