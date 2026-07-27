package com.invasion.client.render;

import com.invasion.entity.EntityIMPrimedTNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.Blocks;

public final class TntProjectileRenderer
        extends EntityRenderer<EntityIMPrimedTNT, TntRenderState> {
    private final BlockModelResolver blockModelResolver;

    public TntProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
        blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public TntRenderState createRenderState() {
        return new TntRenderState();
    }

    @Override
    public void extractRenderState(EntityIMPrimedTNT entity, TntRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.fuseRemainingInTicks = -1.0F;
        blockModelResolver.update(state.blockState, Blocks.TNT.defaultBlockState(),
                TntRenderer.BLOCK_DISPLAY_CONTEXT);
    }

    @Override
    public void submit(TntRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.submitWhiteSolidBlock(state.blockState, poseStack,
                collector, state.lightCoords, false, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
