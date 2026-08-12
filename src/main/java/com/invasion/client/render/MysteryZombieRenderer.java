package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.MysteryZombieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class MysteryZombieRenderer
        extends InvasionZombieRenderer<MysteryZombieEntity> {
    private static final Identifier TEXTURE = InvasionMod.id(
            "textures/entity/zombie/mystery_zombie.png");

    public MysteryZombieRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    public void extractRenderState(
            MysteryZombieEntity entity,
            InvasionZombieRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.brute = false;
    }

    @Override
    public Identifier getTextureLocation(InvasionZombieRenderState state) {
        return TEXTURE;
    }
}
