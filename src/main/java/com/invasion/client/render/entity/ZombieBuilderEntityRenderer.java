package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.ZombieBuilderEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public final class ZombieBuilderEntityRenderer extends BipedEntityRenderer<ZombieBuilderEntity, BipedEntityModel<ZombieBuilderEntity>> {
    private static final Identifier TEXTURE = InvasionMod.id("textures/entity/zombie_builder.png");

    public ZombieBuilderEntityRenderer(Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public Identifier getTexture(ZombieBuilderEntity entity) {
        return TEXTURE;
    }
}
