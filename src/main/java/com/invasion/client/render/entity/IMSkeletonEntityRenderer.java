package com.invasion.client.render.entity;

import com.invasion.entity.IMSkeletonEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Copy of SkeletonEntityRenderer with the entity class changed
 *
 * @see net.minecraft.client.renderer.entity.SkeletonRenderer
 */
public class IMSkeletonEntityRenderer extends HumanoidMobRenderer<IMSkeletonEntity, SkeletonModel<IMSkeletonEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
    private static final ResourceLocation BABY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "tinyskeletons", "textures/entity/skeleton/baby_skeleton.png");

    public IMSkeletonEntityRenderer(EntityRendererProvider.Context context) {
        this(context, ModelLayers.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
    }

    public IMSkeletonEntityRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation layer, ModelLayerLocation legArmorLayer, ModelLayerLocation bodyArmorLayer) {
        this(ctx, legArmorLayer, bodyArmorLayer, new SkeletonModel<>(ctx.bakeLayer(layer)));
    }

    public IMSkeletonEntityRenderer(EntityRendererProvider.Context context, ModelLayerLocation lagArmorLayer, ModelLayerLocation bodyArmorLayer, SkeletonModel<IMSkeletonEntity> model) {
        super(context, model, 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new SkeletonModel<>(context.bakeLayer(lagArmorLayer)), new SkeletonModel<>(context.bakeLayer(bodyArmorLayer)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(IMSkeletonEntity abstractSkeletonEntity) {
        return abstractSkeletonEntity.isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected boolean isShaking(IMSkeletonEntity abstractSkeletonEntity) {
        // TODO:
        return false;//abstractSkeletonEntity.isShaking();
    }
}
