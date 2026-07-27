package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.ThrowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public final class ThrowerRenderer
        extends HumanoidMobRenderer<ThrowerEntity, ThrowerRenderState, ThrowerModel> {
    private static final List<Identifier> TEXTURES = List.of(
            InvasionMod.id("textures/entity/thrower/thrower.png"),
            InvasionMod.id("textures/entity/thrower/thrower_brute.png"));

    public ThrowerRenderer(EntityRendererProvider.Context context) {
        super(context, new ThrowerModel(ThrowerModel.createBodyLayer().bakeRoot()), 1.5F);
    }

    @Override
    public ThrowerRenderState createRenderState() {
        return new ThrowerRenderState();
    }

    @Override
    public void extractRenderState(ThrowerEntity entity, ThrowerRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.tier = entity.getTier();
    }

    @Override
    protected void scale(ThrowerRenderState state, PoseStack poseStack) {
        float scale = state.tier == 2 ? 2.64F : 2.4F;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(ThrowerRenderState state) {
        int index = state.tier - 1;
        return TEXTURES.get(index >= 0 && index < TEXTURES.size() ? index : 0);
    }
}
