package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.IMFatZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class FatZombieRenderer extends MobRenderer<
        IMFatZombieEntity, FatZombieRenderState, FatZombieModel> {
    private static final Identifier TEXTURE =
            InvasionMod.id("textures/entity/fat_zombie.png");

    public FatZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new FatZombieModel(
                FatZombieModel.createBodyLayer().bakeRoot()), 0.8F);
    }

    @Override
    public FatZombieRenderState createRenderState() {
        return new FatZombieRenderState();
    }

    @Override
    public void extractRenderState(IMFatZombieEntity entity,
            FatZombieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.growthScale = entity.getGrowthScale();
        state.eating = entity.isEating();
        state.eatAnimation = entity.getEatAnimation(partialTick);
    }

    @Override
    protected void scale(FatZombieRenderState state, PoseStack poseStack) {
        poseStack.scale(state.growthScale, state.growthScale, state.growthScale);
    }

    @Override
    public Identifier getTextureLocation(FatZombieRenderState state) {
        return TEXTURE;
    }
}
