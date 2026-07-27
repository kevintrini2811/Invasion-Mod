package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.TrapEntityModel;
import com.invasion.entity.TrapEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class TrapEntityRenderer extends EntityRenderer<TrapEntity> {
	private static final Identifier TEXTURE = InvasionMod.id("textures/entity/trap.png");

	private final TrapEntityModel model;

	public TrapEntityRenderer(EntityRendererProvider.Context ctx) {
	    super(ctx);
	    shadowRadius = 0;
	    model = new TrapEntityModel(TrapEntityModel.getTexturedModelData().bakeRoot());
	}

	@Override
    public void render(TrapEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertices, int light) {
	    matrices.pushPose();
	    matrices.mulPose(Axis.XP.rotationDegrees(180));
	    matrices.scale(1.3F, 1.3F, 1.3F);
	    model.setAngles(entity, 0, 0, entity.tickCount + tickDelta, 0, 0);
	    model.renderToBuffer(matrices, vertices.getBuffer(model.renderType(getTexture(entity))), light, 0);
	    matrices.popPose();
	}

	@Override
    public Identifier getTexture(TrapEntity entity) {
		return TEXTURE;
	}
}