package com.invasion.client.render;

import com.invasion.entity.EntityIMPrimedTNT;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public final class TntProjectileRenderer
        extends EntityRenderer<EntityIMPrimedTNT, EntityRenderState> {
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/block/tnt_side.png");
    private final ProjectileCubeModel model =
            new ProjectileCubeModel(ProjectileCubeModel.createLayer(16, 16).bakeRoot());

    public TntProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.25F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                0, -1, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
