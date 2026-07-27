package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.BurrowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class BurrowerRenderer
        extends MobRenderer<BurrowerEntity, LivingEntityRenderState, BurrowerModel> {
    private static final Identifier TEXTURE =
            InvasionMod.id("textures/entity/burrower.png");

    public BurrowerRenderer(EntityRendererProvider.Context context) {
        super(context, new BurrowerModel(BurrowerModel.createBodyLayer().bakeRoot()), 0.45F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(2.2F, 2.2F, 2.2F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
