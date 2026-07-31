package com.invasion.client.render.entity;

import java.util.function.Function;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

/** Renders an equipped armor helmet as armor geometry on a non-humanoid head. */
public final class MobHeadArmorLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private final HumanoidModel<T> armorModel;
    private final Function<T, ModelPart> head;
    private final float scale;

    public MobHeadArmorLayer(RenderLayerParent<T, M> parent,
            EntityRendererProvider.Context context, Function<T, ModelPart> head,
            float scale) {
        super(parent);
        this.armorModel = new HumanoidModel<>(
                context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR));
        this.head = head;
        this.scale = scale;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
            T entity, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch) {
        var stack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(stack.getItem() instanceof ArmorItem armor)
                || armor.getEquipmentSlot() != EquipmentSlot.HEAD) {
            return;
        }

        armorModel.setAllVisible(false);
        armorModel.head.visible = true;
        armorModel.hat.visible = true;
        armorModel.head.setPos(0, 0, 0);
        armorModel.head.setRotation(0, 0, 0);
        armorModel.hat.setPos(0, 0, 0);
        armorModel.hat.setRotation(0, 0, 0);

        poseStack.pushPose();
        head.apply(entity).translateAndRotate(poseStack);
        // A spider head extends eight model pixels forward from its pivot,
        // whereas humanoid armor is centered on its pivot.
        poseStack.translate(0.0F, -0.38F, -0.25F);
        poseStack.scale(scale, scale, scale);
        boolean inner = false;
        var material = armor.getMaterial().value();
        for (var layer : material.layers()) {
            var texture = net.neoforged.neoforge.client.ClientHooks.getArmorTexture(
                    entity, stack, layer, inner, EquipmentSlot.HEAD);
            var model = net.neoforged.neoforge.client.ClientHooks.getArmorModel(
                    entity, stack, EquipmentSlot.HEAD, armorModel);
            model.renderToBuffer(poseStack,
                    buffers.getBuffer(RenderType.armorCutoutNoCull(texture)),
                    light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        poseStack.popPose();
    }
}
