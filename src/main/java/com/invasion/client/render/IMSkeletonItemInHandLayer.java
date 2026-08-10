package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** Keeps held items aligned with baby-transformed skeleton arms. */
final class IMSkeletonItemInHandLayer<
        S extends ArmedEntityRenderState,
        M extends EntityModel<S> & ArmedModel<S>>
        extends ItemInHandLayer<S, M> {
    IMSkeletonItemInHandLayer(RenderLayerParent<S, M> parent) {
        super(parent);
    }

    @Override
    protected void submitArmWithItem(
            S state, ItemStackRenderState itemState, ItemStack stack,
            HumanoidArm arm, PoseStack poseStack,
            SubmitNodeCollector collector, int light) {
        // The geometry is already transformed into baby proportions. Vanilla's
        // additional baby item offset pulls the bow back into the torso.
        boolean baby = state.isBaby;
        state.isBaby = false;
        try {
            super.submitArmWithItem(
                    state, itemState, stack, arm, poseStack, collector, light);
        } finally {
            state.isBaby = baby;
        }
    }
}
