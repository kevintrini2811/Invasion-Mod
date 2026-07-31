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
    private final float offsetY;
    private final float offsetZ;

    public MobHeadArmorLayer(RenderLayerParent<T, M> parent,
            EntityRendererProvider.Context context, Function<T, ModelPart> head,
            float scale) {
        this(parent, context, head, scale, -0.62F, -0.25F);
    }

    public MobHeadArmorLayer(RenderLayerParent<T, M> parent,
            EntityRendererProvider.Context context, Function<T, ModelPart> head,
            float scale, float offsetY, float offsetZ) {
        super(parent);
        this.armorModel = new HumanoidModel<>(
                context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR));
        this.head = head;
        this.scale = scale;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
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
        poseStack.translate(0.0F, offsetY, offsetZ);
        poseStack.scale(scale, scale, scale);
        String base = "textures/models/armor/"
                + armor.getMaterial().getName() + "_layer_1.png";
        String resolved = net.minecraftforge.client.ForgeHooksClient.getArmorTexture(
                entity, stack, base, EquipmentSlot.HEAD, null);
        var texture = new net.minecraft.resources.ResourceLocation(resolved);
        var model = net.minecraftforge.client.ForgeHooksClient.getArmorModel(
                entity, stack, EquipmentSlot.HEAD, armorModel);
        model.renderToBuffer(poseStack,
                buffers.getBuffer(RenderType.armorCutoutNoCull(texture)),
                light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        poseStack.popPose();
    }
}
