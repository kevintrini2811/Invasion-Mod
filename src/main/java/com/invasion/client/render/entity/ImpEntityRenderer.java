package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.ImpEntityModel;
import com.invasion.entity.ImpEnitty;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

public class ImpEntityRenderer extends LivingEntityRenderer<ImpEnitty, ImpEntityModel> {
	private static final Identifier TEXTURE = InvasionMod.id("textures/entity/imp.png");

	public ImpEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ImpEntityModel(ImpEntityModel.getTexturedModelData().bakeRoot()), 0.3F);
	}

	@Override
    public Identifier getTexture(ImpEnitty entity) {
		return TEXTURE;
	}
}