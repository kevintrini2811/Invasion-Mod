package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.EntityIMGiantBird;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class VultureRenderer
        extends MobRenderer<EntityIMGiantBird, VultureRenderState, VultureModel> {
    private static final Identifier TEXTURE =
            InvasionMod.id("textures/entity/vulture.png");

    public VultureRenderer(EntityRendererProvider.Context context) {
        super(context, new VultureModel(VultureModel.createBodyLayer().bakeRoot()), 0.9F);
    }

    @Override
    public VultureRenderState createRenderState() {
        return new VultureRenderState();
    }

    @Override
    public void extractRenderState(
            EntityIMGiantBird entity, VultureRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.roll = entity.getRoll(tickDelta);
        state.clawsForward = entity.getClawsForward();
        state.beakOpen = entity.isBeakOpen();
    }

    @Override
    public Identifier getTextureLocation(VultureRenderState state) {
        return TEXTURE;
    }
}
