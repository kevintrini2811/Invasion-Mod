package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusSpiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

public class IMSpiderEntityRenderer<T extends NexusSpiderEntity> extends SpiderRenderer<T> {
    public static final ResourceLocation JUMPER = InvasionMod.id("textures/entity/spider/jumping_spider.png");
    public static final ResourceLocation MOTHER = InvasionMod.id("textures/entity/spider/mother_spider.png");
    public static final ResourceLocation NORMAL = ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");
    public static final ResourceLocation CAVE = ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png");

    private final ResourceLocation texture;
    private final float scale;

    public IMSpiderEntityRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        this(context, texture, 1.0F);
    }

    public IMSpiderEntityRenderer(EntityRendererProvider.Context context,
            ResourceLocation texture, float scale) {
        super(context);
        this.texture = texture;
        this.scale = scale;
        addLayer(new MobHeadArmorLayer<>(this, context,
                spider -> getModel().root().getChild("head"), 1.08F));
    }

    @Override
    public ResourceLocation getTextureLocation(T spiderEntity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float renderedScale = scale * entity.scaleAmount();
        poseStack.scale(renderedScale, renderedScale, renderedScale);
    }
}
