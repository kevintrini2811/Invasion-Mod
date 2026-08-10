package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Uses baby-transformed skeleton armor without applying a second baby layer. */
final class IMSkeletonArmorLayer<
        S extends HumanoidRenderState, M extends HumanoidModel<S>,
        A extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final HumanoidArmorLayer<S, M, A> adultLayer;
    private final HumanoidArmorLayer<S, M, A> babyLayer;

    IMSkeletonArmorLayer(
            RenderLayerParent<S, M> parent,
            ArmorModelSet<A> adultModels,
            ArmorModelSet<A> babyModels,
            EquipmentLayerRenderer equipmentRenderer) {
        super(parent);
        adultLayer = new HumanoidArmorLayer<>(
                parent, adultModels, equipmentRenderer);
        babyLayer = new HumanoidArmorLayer<>(
                parent, babyModels, equipmentRenderer);
    }

    @Override
    public void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light,
            S state, float yRot, float xRot) {
        if (!state.isBaby) {
            adultLayer.submit(
                    poseStack, collector, light, state, yRot, xRot);
            return;
        }

        // Tiny Skeletons uses adult equipment layers on geometry that has
        // already been transformed to baby proportions.
        state.isBaby = false;
        try {
            babyLayer.submit(
                    poseStack, collector, light, state, yRot, xRot);
        } finally {
            state.isBaby = true;
        }
    }
}
