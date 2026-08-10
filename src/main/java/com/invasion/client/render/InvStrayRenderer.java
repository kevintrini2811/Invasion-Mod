package com.invasion.client.render;

import com.invasion.entity.IMStrayEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class InvStrayRenderer extends HumanoidMobRenderer<
        IMStrayEntity, SkeletonRenderState,
        SkeletonModel<SkeletonRenderState>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/skeleton/stray.png");
    private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace(
            "textures/entity/skeleton/stray_overlay.png");

    public InvStrayRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new SkeletonModel<>(context.bakeLayer(ModelLayers.STRAY)),
                IMBabySkeletonModels.skeleton(), 0.5F);
        ArmorModelSet<SkeletonModel<SkeletonRenderState>> armor =
                ArmorModelSet.bake(
                        ModelLayers.STRAY_ARMOR,
                        context.getModelSet(),
                        SkeletonModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, armor, context.getEquipmentRenderer()));
        addLayer(new SkeletonClothingLayer<>(
                this, context.getModelSet(),
                ModelLayers.STRAY_OUTER_LAYER, OVERLAY));
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public void extractRenderState(
            IMStrayEntity entity, SkeletonRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(
            IMStrayEntity entity, HumanoidArm arm) {
        if (arm == entity.getMainArm()
                && entity.isAggressive()
                && entity.getMainHandItem().is(Items.BOW)) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public ResourceLocation getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}
