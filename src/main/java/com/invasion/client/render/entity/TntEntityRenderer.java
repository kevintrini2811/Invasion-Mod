package com.invasion.client.render.entity;

import com.invasion.entity.EntityIMPrimedTNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class TntEntityRenderer extends EntityRenderer<EntityIMPrimedTNT> {
    private final BlockRenderDispatcher blockRenderManager;

    public TntEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderManager = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(EntityIMPrimedTNT tntEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i) {
        matrixStack.pushPose();
        matrixStack.translate(0.0F, 0.5F, 0.0F);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        matrixStack.translate(-0.5F, -0.5F, 0.5F);
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderManager, Blocks.TNT.defaultBlockState(), matrixStack, vertexConsumerProvider, i, false);
        matrixStack.popPose();
        super.render(tntEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTexture(EntityIMPrimedTNT tntEntity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}