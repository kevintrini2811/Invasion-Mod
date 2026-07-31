package com.invasion.client.render;

import com.invasion.entity.IMZombieVillagerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BabyZombieVillagerModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.resources.ResourceLocation;

public final class IMZombieVillagerRenderer extends HumanoidMobRenderer<
        IMZombieVillagerEntity,
        ZombieVillagerRenderState,
        HumanoidModel<ZombieVillagerRenderState>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie_villager/zombie_villager.png");
    private static final ResourceLocation BABY_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/zombie_villager/zombie_villager_baby.png");

    public IMZombieVillagerRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new ZombieVillagerModel<>(
                        context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)),
                new BabyZombieVillagerModel<>(
                        context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY)),
                0.5F);

        ArmorModelSet<HumanoidModel<ZombieVillagerRenderState>> adultArmor =
                ArmorModelSet.bake(
                        ModelLayers.ZOMBIE_VILLAGER_ARMOR,
                        context.getModelSet(),
                        HumanoidModel::new);
        ArmorModelSet<HumanoidModel<ZombieVillagerRenderState>> babyArmor =
                ArmorModelSet.bake(
                        ModelLayers.ZOMBIE_VILLAGER_BABY_ARMOR,
                        context.getModelSet(),
                        HumanoidModel::new);
        addLayer(new HumanoidArmorLayer<>(
                this, adultArmor, babyArmor, context.getEquipmentRenderer()));
    }

    @Override
    public ZombieVillagerRenderState createRenderState() {
        return new ZombieVillagerRenderState();
    }

    @Override
    public void extractRenderState(
            IMZombieVillagerEntity entity,
            ZombieVillagerRenderState state,
            float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieVillagerRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }
}
