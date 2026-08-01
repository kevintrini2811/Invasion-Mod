package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.EntityIMSpeedyZombie;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class SpeedyZombieRenderer
        extends InvasionZombieRenderer<EntityIMSpeedyZombie> {
    private static final Identifier TEXTURE = InvasionMod.id(
            "textures/entity/zombie/speedy_zombie.png");

    public SpeedyZombieRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    public void extractRenderState(
            EntityIMSpeedyZombie entity,
            InvasionZombieRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        // The supplied skin targets the standard humanoid zombie geometry.
        state.brute = false;
    }

    @Override
    public Identifier getTextureLocation(InvasionZombieRenderState state) {
        return TEXTURE;
    }
}
