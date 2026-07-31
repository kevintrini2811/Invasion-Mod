package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusSpiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

public class IMSpiderEntityRenderer<T extends NexusSpiderEntity> extends SpiderRenderer<T> {
    public static final ResourceLocation JUMPER = InvasionMod.id("textures/entity/spider/jumping_spider.png");
    public static final ResourceLocation MOTHER = InvasionMod.id("textures/entity/spider/mother_spider.png");

    private final ResourceLocation texture;

    public IMSpiderEntityRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTexture(T spiderEntity) {
        return texture;
    }
}