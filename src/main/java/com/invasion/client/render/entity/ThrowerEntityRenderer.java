package com.invasion.client.render.entity;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import com.invasion.InvasionMod;
import com.invasion.client.render.entity.model.ThrowerEntityModel;
import com.invasion.entity.ThrowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;

public class ThrowerEntityRenderer extends HumanoidMobRenderer<ThrowerEntity, ThrowerEntityModel> {
	private static final List<Identifier> TEXTURES = Stream.of(
	        "textures/entity/thrower/thrower.png",
	        "textures/entity/thrower/thrower_brute.png"
    ).map(InvasionMod::id).toList();

	public ThrowerEntityRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ThrowerEntityModel(ThrowerEntityModel.getTexturedModelData().bakeRoot()), 1.5F);
	}

    @Override
    protected void scale(ThrowerEntity entity, PoseStack matrices, float amount) {
        matrices.scale(2.4F, 2.4F, 2.4F);
    }

	@Override
    public Identifier getTexture(ThrowerEntity entity) {
	    int id = entity.getTier() - 1;
	    return TEXTURES.get(id < 0 || id >= TEXTURES.size() ? 0 : id);
	}

}