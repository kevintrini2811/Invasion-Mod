package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders a carried skull centered between a baby wither skeleton's arms. */
final class IMWitherSkeletonSkullLayer extends RenderLayer<
        IMWitherSkeletonRenderState, IMWitherSkeletonModel> {
    IMWitherSkeletonSkullLayer(InvWitherSkeletonRenderer parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light,
            IMWitherSkeletonRenderState state, float yRot, float xRot) {
        if (state.skullModel.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.075F, 0.325F);
        poseStack.translate(0.0F, 0.6875F, -0.75F);
        poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(0.25F, 0.1875F, 0.25F);
        poseStack.scale(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        state.skullModel.submit(
                poseStack, collector, light,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
