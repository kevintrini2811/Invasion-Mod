package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.PigmanEngineerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class PigmanEngineerEntityRenderer extends HumanoidMobRenderer<PigmanEngineerEntity, HumanoidModel<PigmanEngineerEntity>> {
    private static final Identifier TEXTURE = InvasionMod.id("textures/entity/pigman_engineer.png");

    public PigmanEngineerEntityRenderer(Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public Identifier getTexture(PigmanEngineerEntity entity) {
        return TEXTURE;
    }
}