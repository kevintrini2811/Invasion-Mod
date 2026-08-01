package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.AbstractIMZombieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class SpeedyZombieEntityRenderer
        extends AbstractIMZombieEntityRenderer {
    private static final ResourceLocation TEXTURE = InvasionMod.id(
            "textures/entity/zombie/speedy_zombie.png");

    public SpeedyZombieEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractIMZombieEntity entity) {
        return TEXTURE;
    }
}
