package com.invasion.client.render;

import com.invasion.entity.NexusSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class TexturedSpiderRenderer<T extends NexusSpiderEntity>
        extends MobRenderer<T, InvasionSpiderRenderState, SpiderModel> {
    private final Identifier texture;

    public TexturedSpiderRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new SpiderModel(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
        this.texture = texture;
    }

    @Override
    public InvasionSpiderRenderState createRenderState() {
        return new InvasionSpiderRenderState();
    }

    @Override
    public void extractRenderState(T entity, InvasionSpiderRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.invasionScale = entity.scaleAmount();
    }

    @Override
    protected void scale(InvasionSpiderRenderState state, PoseStack poseStack) {
        poseStack.scale(state.invasionScale, state.invasionScale, state.invasionScale);
    }

    @Override
    public Identifier getTextureLocation(InvasionSpiderRenderState state) {
        return texture;
    }
}
