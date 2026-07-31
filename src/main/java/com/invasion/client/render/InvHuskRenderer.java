package com.invasion.client.render;

import com.invasion.entity.IMHuskEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BabyZombieModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;

public final class InvHuskRenderer extends HumanoidMobRenderer<
        IMHuskEntity, ZombieRenderState,
        HumanoidModel<ZombieRenderState>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie/husk.png");
    private static final ResourceLocation BABY_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie/husk_baby.png");

    public InvHuskRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.HUSK)),
                new BabyZombieModel<>(
                        context.bakeLayer(ModelLayers.HUSK_BABY)),
                0.5F);
        ArmorModelSet<HumanoidModel<ZombieRenderState>> adultArmor =
                ArmorModelSet.bake(
                        ModelLayers.HUSK_ARMOR,
                        context.getModelSet(),
                        HumanoidModel::new);
        ArmorModelSet<HumanoidModel<ZombieRenderState>> babyArmor =
                ArmorModelSet.bake(
                        ModelLayers.HUSK_BABY_ARMOR,
                        context.getModelSet(),
                        HumanoidModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, adultArmor, babyArmor,
                context.getEquipmentRenderer()));
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public void extractRenderState(
            IMHuskEntity entity, ZombieRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
        state.isConverting = entity.isUnderWaterConverting();
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }
}
