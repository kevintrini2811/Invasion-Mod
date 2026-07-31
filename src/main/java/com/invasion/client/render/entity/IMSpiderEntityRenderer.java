package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusSpiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

public class IMSpiderEntityRenderer<T extends NexusSpiderEntity> extends SpiderRenderer<T> {
    public static final ResourceLocation JUMPER = InvasionMod.id("textures/entity/spider/jumping_spider.png");
    public static final ResourceLocation MOTHER = InvasionMod.id("textures/entity/spider/mother_spider.png");
    public static final ResourceLocation NORMAL = ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");
    public static final ResourceLocation CAVE = ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png");

    private final ResourceLocation texture;

    public IMSpiderEntityRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context);
        this.texture = texture;
        addLayer(new RenderLayer<T, net.minecraft.client.model.SpiderModel<T>>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                    T spider, float limbSwing, float limbSwingAmount, float partialTick,
                    float ageInTicks, float netHeadYaw, float headPitch) {
                var helmet = spider.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet.isEmpty()) return;
                poseStack.pushPose();
                getParentModel().root().getChild("head").translateAndRotate(poseStack);
                poseStack.translate(0.0F, -0.55F, -0.15F);
                poseStack.scale(0.85F, 0.85F, 0.85F);
                context.getItemRenderer().renderStatic(
                        spider, helmet, ItemDisplayContext.HEAD, false,
                        poseStack, buffers, spider.level(), light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        spider.getId());
                poseStack.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(T spiderEntity) {
        return texture;
    }
}
