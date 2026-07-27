package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.TrapEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class TrapRenderer extends EntityRenderer<TrapEntity, TrapRenderState> {
    private static final Identifier TEXTURE = InvasionMod.id("textures/entity/trap.png");
    private final TrapModel model;

    public TrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
        model = new TrapModel(TrapModel.createBodyLayer().bakeRoot());
    }

    @Override
    public TrapRenderState createRenderState() {
        return new TrapRenderState();
    }

    @Override
    public void extractRenderState(TrapEntity entity, TrapRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.trapType = entity.getTrapType();
    }

    @Override
    public void submit(TrapRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.scale(1.3F, 1.3F, 1.3F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                0, -1, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
