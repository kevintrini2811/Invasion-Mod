package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.PigmanEngineerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class PigmanEngineerEntityRenderer extends HumanoidMobRenderer<PigmanEngineerEntity, HumanoidModel<PigmanEngineerEntity>> {
    private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/pigman_engineer.png");

    public PigmanEngineerEntityRenderer(Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(PigmanEngineerEntity entity) {
        return TEXTURE;
    }
}
