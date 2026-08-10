package com.invasion.client.render;

import com.invasion.entity.IMBoggedEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BoggedModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.client.renderer.entity.state.BoggedRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class InvBoggedRenderer extends HumanoidMobRenderer<
        IMBoggedEntity, BoggedRenderState,
        SkeletonModel<BoggedRenderState>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/skeleton/bogged.png");
    private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace(
            "textures/entity/skeleton/bogged_overlay.png");

    public InvBoggedRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new BoggedModel(context.bakeLayer(ModelLayers.BOGGED)),
                IMBabySkeletonModels.skeleton(), 0.5F);
        ArmorModelSet<SkeletonModel<BoggedRenderState>> armor =
                ArmorModelSet.bake(
                        ModelLayers.BOGGED_ARMOR,
                        context.getModelSet(),
                        SkeletonModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, armor, context.getEquipmentRenderer()));
        addLayer(new SkeletonClothingLayer<>(
                this, context.getModelSet(),
                ModelLayers.BOGGED_OUTER_LAYER, OVERLAY));
    }

    @Override
    public BoggedRenderState createRenderState() {
        return new BoggedRenderState();
    }

    @Override
    public void extractRenderState(
            IMBoggedEntity entity, BoggedRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
        state.isSheared = entity.isSheared();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(
            IMBoggedEntity entity, HumanoidArm arm) {
        if (arm == entity.getMainArm()
                && entity.isAggressive()
                && entity.getMainHandItem().is(Items.BOW)) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public ResourceLocation getTextureLocation(BoggedRenderState state) {
        return TEXTURE;
    }
}
