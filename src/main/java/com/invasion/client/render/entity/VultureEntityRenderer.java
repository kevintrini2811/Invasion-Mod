package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.ModelBird;
import com.invasion.entity.VultureEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

public class VultureEntityRenderer extends LivingEntityRenderer<VultureEntity, ModelBird> {
	private static final Identifier TEXTURE = InvasionMod.id("textures/bird_tx1.png");

	public VultureEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ModelBird(ModelBird.getTexturedModelData().bakeRoot()), 0.4F);
	}

    @Override
    public Identifier getTexture(VultureEntity entity) {
        return TEXTURE;
    }

	/*private void renderNavigationVector(EntityIMBird entityBird, double entityRenderOffsetX, double entityRenderOffsetY, double entityRenderOffsetZ) {
		Tessellator tessellator = Tessellator.instance;
		GL11.glPushMatrix();

		GL11.glDisable(3553);
		GL11.glDisable(2896);
		GL11.glEnable(3042);
		GL11.glBlendFunc(770, 1);

		Vec3 target = entityBird.getFlyTarget();
		double drawWidth = 0.1D;

		tessellator.startDrawing(5);
		tessellator.setColorRGBA_F(1.0F, 0.0F, 0.0F, 1.0F);
		for (int j = 0; j < 5; j++) {
			double xOffset = drawWidth;
			double zOffset = drawWidth;
			if ((j == 1) || (j == 2)) {
				xOffset += drawWidth * 2.0D;
			}
			if ((j == 2) || (j == 3)) {
				zOffset += drawWidth * 2.0D;
			}
			tessellator.addVertex(entityRenderOffsetX - entityBird.width / 2.0F + xOffset, entityRenderOffsetY + entityBird.height / 2.0F, entityRenderOffsetZ - entityBird.width / 2.0F + zOffset);
			tessellator.addVertex(target.xCoord + xOffset - RenderManager.renderPosX, target.yCoord - RenderManager.renderPosY, target.zCoord + zOffset - RenderManager.renderPosZ);
		}
		tessellator.draw();

		GL11.glDisable(3042);
		GL11.glEnable(2896);
		GL11.glEnable(3553);

		GL11.glPopMatrix();
	}*/
}