package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.ImpEnitty;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ImpRenderer
        extends MobRenderer<ImpEnitty, LivingEntityRenderState, ImpModel> {
    private static final Identifier TEXTURE = InvasionMod.id("textures/entity/imp.png");

    public ImpRenderer(EntityRendererProvider.Context context) {
        super(context, new ImpModel(ImpModel.createBodyLayer().bakeRoot()), 0.3F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
