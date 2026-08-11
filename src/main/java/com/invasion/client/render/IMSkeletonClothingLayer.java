package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.ResourceLocation;

/** Selects matching adult and baby geometry for skeleton clothing overlays. */
final class IMSkeletonClothingLayer<
        S extends SkeletonRenderState, M extends EntityModel<S>>
        extends RenderLayer<S, M> {
    private final SkeletonModel<S> adultModel;
    private final SkeletonModel<S> babyModel;
    private final ResourceLocation texture;

    IMSkeletonClothingLayer(
            RenderLayerParent<S, M> parent,
            SkeletonModel<S> adultModel,
            SkeletonModel<S> babyModel,
            ResourceLocation texture) {
        super(parent);
        this.adultModel = adultModel;
        this.babyModel = babyModel;
        this.texture = texture;
    }

    @Override
    public void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light,
            S state, float yRot, float xRot) {
        coloredCutoutModelCopyLayerRender(
                state.isBaby ? babyModel : adultModel,
                texture, poseStack, collector, light, state, -1, 1);
    }
}
