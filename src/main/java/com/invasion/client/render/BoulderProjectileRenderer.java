package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.BoulderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class BoulderProjectileRenderer
        extends EntityRenderer<BoulderEntity, EntityRenderState> {
    private static final class NoOutlineRenderState extends EntityRenderState {
        @Override
        public boolean appearsGlowing() {
            return false;
        }
    }

    private static final Identifier TEXTURE =
            InvasionMod.id("textures/entity/boulder.png");
    private final ProjectileCubeModel model =
            new ProjectileCubeModel(ProjectileCubeModel.createLayer(64, 32).bakeRoot());

    public BoulderProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.25F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new NoOutlineRenderState();
    }

    @Override
    public void extractRenderState(BoulderEntity entity, EntityRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.outlineColor = EntityRenderState.NO_OUTLINE;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 12.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 7.0F));
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                0, -1, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
