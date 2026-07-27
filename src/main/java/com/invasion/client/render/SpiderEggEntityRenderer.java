package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.SpiderEggEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class SpiderEggEntityRenderer extends EntityRenderer<SpiderEggEntity, EntityRenderState> {
	private static final Identifier TEXTURE = InvasionMod.id("textures/entity/spider_egg.png");

	private final EggModel model = new EggModel(EggModel.getTexturedModelData().bakeRoot());

	public SpiderEggEntityRenderer(EntityRendererProvider.Context context) {
	    super(context);
        shadowRadius = 0.35F;
	}

	@Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords,
                0, -1, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }
}
