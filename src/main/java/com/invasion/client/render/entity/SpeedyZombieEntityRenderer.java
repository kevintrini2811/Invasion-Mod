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
    protected boolean isBrute(AbstractIMZombieEntity entity) {
        // The supplied skin targets the standard humanoid zombie geometry.
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractIMZombieEntity entity) {
        return TEXTURE;
    }
}
