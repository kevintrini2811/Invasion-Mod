package com.invasion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/** Renders humanoid helmet equipment on non-humanoid IM mob models. */
public final class HeadArmorLayer<
        S extends LivingEntityRenderState,
        M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private final HeadArmorModel<S> armorModel;
    private final EquipmentLayerRenderer equipmentRenderer;
    private final Function<S, ItemStack> helmet;
    private final float offsetY;
    private final float offsetZ;

    public HeadArmorLayer(
            RenderLayerParent<S, M> parent,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            Function<S, ItemStack> helmet) {
        this(parent, context, helmet, 0.0F, 0.0F);
    }

    public HeadArmorLayer(
            RenderLayerParent<S, M> parent,
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            Function<S, ItemStack> helmet,
            float offsetY,
            float offsetZ) {
        super(parent);
        armorModel = new HeadArmorModel<>(
                HeadArmorModel.createBodyLayer().bakeRoot());
        equipmentRenderer = context.getEquipmentRenderer();
        this.helmet = helmet;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light,
            S state, float yRot, float xRot) {
        ItemStack stack = helmet.apply(state);
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null
                || equippable.slot() != EquipmentSlot.HEAD
                || equippable.assetId().isEmpty()) {
            return;
        }

        armorModel.resetPose();
        armorModel.head.loadPose(
                getParentModel().root().getChild("head").storePose());
        armorModel.head.y += offsetY;
        armorModel.head.z += offsetZ;
        armorModel.head.setInitialPose(armorModel.head.storePose());

        equipmentRenderer.renderLayers(
                EquipmentClientInfo.LayerType.HUMANOID,
                equippable.assetId().orElseThrow(),
                (net.minecraft.client.model.Model) armorModel,
                state,
                stack,
                poseStack,
                collector,
                light,
                state.outlineColor);
    }
}
