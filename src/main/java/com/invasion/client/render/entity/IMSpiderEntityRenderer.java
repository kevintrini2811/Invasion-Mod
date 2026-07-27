package com.invasion.client.render.entity;

import com.invasion.InvasionMod;
import com.invasion.entity.NexusSpiderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.Identifier;

public class IMSpiderEntityRenderer<T extends NexusSpiderEntity> extends SpiderRenderer<T> {
    public static final Identifier JUMPER = InvasionMod.id("textures/entity/spider/jumping_spider.png");
    public static final Identifier MOTHER = InvasionMod.id("textures/entity/spider/mother_spider.png");

    private final Identifier texture;

    public IMSpiderEntityRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(T spiderEntity) {
        return texture;
    }
}