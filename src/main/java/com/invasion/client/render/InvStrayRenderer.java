package com.invasion.client.render;

import com.invasion.entity.IMStrayEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class InvStrayRenderer extends HumanoidMobRenderer<
        IMStrayEntity, SkeletonRenderState,
        SkeletonModel<SkeletonRenderState>> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/skeleton/stray.png");
    private static final Identifier OVERLAY = Identifier.withDefaultNamespace(
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
        addLayer(new IMSkeletonArmorLayer<>(
                this, armor,
                IMBabySkeletonModels.armor(SkeletonModel::new, 1.0F),
                context.getEquipmentRenderer()));
        layers.removeIf(ItemInHandLayer.class::isInstance);
        addLayer(new IMSkeletonItemInHandLayer<>(this));
        addLayer(new IMSkeletonClothingLayer<>(
                this,
                new SkeletonModel<>(context.bakeLayer(
                        ModelLayers.STRAY_OUTER_LAYER)),
                IMBabySkeletonModels.clothing(0.25F), OVERLAY));
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
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}
