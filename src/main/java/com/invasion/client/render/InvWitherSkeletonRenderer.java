package com.invasion.client.render;

import com.invasion.entity.IMWitherSkeletonEntity;
import com.invasion.entity.EquipmentUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

public final class InvWitherSkeletonRenderer extends
        HumanoidMobRenderer<IMWitherSkeletonEntity,
                IMWitherSkeletonRenderState, IMWitherSkeletonModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/skeleton/wither_skeleton.png");

    public InvWitherSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context,
                new IMWitherSkeletonModel(
                        context.bakeLayer(ModelLayers.WITHER_SKELETON)),
                IMBabySkeletonModels.witherSkeleton(), 0.5F);
        ArmorModelSet<IMWitherSkeletonModel> armor =
                ArmorModelSet.bake(
                        ModelLayers.WITHER_SKELETON_ARMOR,
                        context.getModelSet(),
                        IMWitherSkeletonModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, armor, context.getEquipmentRenderer()));
        layers.removeIf(ItemInHandLayer.class::isInstance);
        addLayer(new IMSkeletonItemInHandLayer<>(this));
    }

    @Override
    public IMWitherSkeletonRenderState createRenderState() {
        return new IMWitherSkeletonRenderState();
    }

    @Override
    public void extractRenderState(
            IMWitherSkeletonEntity entity,
            IMWitherSkeletonRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow =
                EquipmentUtil.isRangedWeapon(entity.getMainHandItem());
        state.groupLeaderWaiting = entity.isGroupLeaderWaiting();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(
            IMWitherSkeletonEntity entity, HumanoidArm arm) {
        if (arm == entity.getMainArm()
                && entity.isAggressive()
                && EquipmentUtil.isRangedWeapon(entity.getMainHandItem())) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public Identifier getTextureLocation(IMWitherSkeletonRenderState state) {
        return TEXTURE;
    }
}
