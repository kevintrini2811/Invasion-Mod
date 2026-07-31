package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.ImpEntityModel;
import com.invasion.entity.ImpEnitty;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class ImpEntityRenderer extends LivingEntityRenderer<ImpEnitty, ImpEntityModel> {
	private static final ResourceLocation TEXTURE = InvasionMod.id("textures/entity/imp.png");

	public ImpEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ImpEntityModel(ImpEntityModel.getTexturedModelData().bakeRoot()), 0.3F);
        addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(
                this, ctx.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(ImpEnitty entity) {
		return TEXTURE;
	}

    @Override
    protected boolean shouldShowName(ImpEnitty entity) {
        return entity.hasCustomName() && super.shouldShowName(entity);
    }
}
