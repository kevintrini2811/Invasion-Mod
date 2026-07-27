package com.invasion.client.render;

import com.invasion.entity.IMSkeletonEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;

public final class InvSkeletonRenderer extends
        HumanoidMobRenderer<IMSkeletonEntity, SkeletonRenderState, SkeletonModel<SkeletonRenderState>> {
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    public InvSkeletonRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
        ArmorModelSet<SkeletonModel<SkeletonRenderState>> armor = ArmorModelSet.bake(
                ModelLayers.SKELETON_ARMOR, context.getModelSet(), SkeletonModel::new);
        addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public SkeletonRenderState createRenderState() {
        return new SkeletonRenderState();
    }

    @Override
    public void extractRenderState(IMSkeletonEntity entity, SkeletonRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.isAggressive = entity.isAggressive();
    }

    @Override
    public Identifier getTextureLocation(SkeletonRenderState state) {
        return TEXTURE;
    }
}
