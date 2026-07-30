package com.invasion.client.render;

import com.invasion.entity.IMParchedEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class InvParchedRenderer extends HumanoidMobRenderer<
        IMParchedEntity, SkeletonRenderState,
        SkeletonModel<SkeletonRenderState>> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/skeleton/parched.png");

    public InvParchedRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new SkeletonModel<>(
                        context.bakeLayer(ModelLayers.PARCHED)),
                0.5F);
        ArmorModelSet<SkeletonModel<SkeletonRenderState>> armor =
                ArmorModelSet.bake(
                        ModelLayers.PARCHED_ARMOR,
                        context.getModelSet(),
                        SkeletonModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public void extractRenderState(
            IMParchedEntity entity, SkeletonRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
        state.isHoldingBow = entity.getMainHandItem().is(Items.BOW);
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(
            IMParchedEntity entity, HumanoidArm arm) {
        if (arm == entity.getMainArm()
                && entity.isAggressive()
                && entity.getMainHandItem().is(Items.BOW)) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}
