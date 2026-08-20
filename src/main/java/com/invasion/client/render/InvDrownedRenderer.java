package com.invasion.client.render;

import com.invasion.entity.IMDrownedEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BabyDrownedModel;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

public final class InvDrownedRenderer extends HumanoidMobRenderer<
        IMDrownedEntity, ZombieRenderState,
        DrownedModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie/drowned.png");
    private static final ResourceLocation BABY_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie/drowned_baby.png");

    public InvDrownedRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new DrownedModel(context.bakeLayer(ModelLayers.DROWNED)),
                new BabyDrownedModel(
                        context.bakeLayer(ModelLayers.DROWNED_BABY)),
                0.5F);
        ArmorModelSet<DrownedModel> adultArmor =
                ArmorModelSet.bake(
                        ModelLayers.DROWNED_ARMOR,
                        context.getModelSet(),
                        DrownedModel::new);
        ArmorModelSet<DrownedModel> babyArmor =
                ArmorModelSet.bake(
                        ModelLayers.DROWNED_BABY_ARMOR,
                        context.getModelSet(),
                        BabyDrownedModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, adultArmor, babyArmor,
                context.getEquipmentRenderer()));
        addLayer(new DrownedOuterLayer(this, context.getModelSet()));
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public void extractRenderState(
            IMDrownedEntity entity, ZombieRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(
            IMDrownedEntity entity, HumanoidArm arm) {
        if (arm == entity.getMainArm()
                && entity.isAggressive()
                && entity.getItemHeldByArm(arm).is(Items.TRIDENT)) {
            return HumanoidModel.ArmPose.THROW_TRIDENT;
        }
        return super.getArmPose(entity, arm);
    }

    @Override
    protected void setupRotations(
            ZombieRenderState state, PoseStack poseStack,
            float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.swimAmount > 0.0F) {
            float rotation = Mth.lerp(
                    state.swimAmount, 0.0F, -10.0F - state.xRot);
            poseStack.rotateAround(
                    Axis.XP.rotationDegrees(rotation),
                    0.0F, state.boundingBoxHeight / 2.0F / scale, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }
}
