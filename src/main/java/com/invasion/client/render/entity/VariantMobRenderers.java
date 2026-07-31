package com.invasion.client.render.entity;

import java.util.List;
import com.invasion.entity.AbstractIMZombieEntity;
import com.invasion.entity.IMCaveSpiderEntity;
import com.invasion.entity.IMEndermanEntity;
import com.invasion.entity.IMZombifiedPiglinEntity;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

/** Immediate-mode renderers for vanilla-derived invasion variants on 1.21.1. */
public final class VariantMobRenderers {
    private VariantMobRenderers() {}

    private abstract static class ZombieVariant extends AbstractIMZombieEntityRenderer {
        private final List<ResourceLocation> textures;
        ZombieVariant(EntityRendererProvider.Context context, String texture) {
            super(context);
            textures = List.of(ResourceLocation.withDefaultNamespace(texture));
        }
        @Override protected List<ResourceLocation> getTextures() { return textures; }
        @Override protected boolean isBrute(AbstractIMZombieEntity entity) { return false; }
    }
    public static final class Husk extends ZombieVariant {
        public Husk(EntityRendererProvider.Context c) { super(c, "textures/entity/zombie/husk.png"); }
    }
    public static final class Drowned extends ZombieVariant {
        public Drowned(EntityRendererProvider.Context c) { super(c, "textures/entity/zombie/drowned.png"); }
    }
    public static final class ZombieVillager extends ZombieVariant {
        public ZombieVillager(EntityRendererProvider.Context c) { super(c, "textures/entity/zombie_villager/zombie_villager.png"); }
    }

    private abstract static class SkeletonVariant extends IMSkeletonEntityRenderer {
        private final ResourceLocation texture;
        SkeletonVariant(EntityRendererProvider.Context c, String texture) {
            super(c);
            this.texture = ResourceLocation.withDefaultNamespace(texture);
        }
        @Override public ResourceLocation getTextureLocation(com.invasion.entity.IMSkeletonEntity e) { return texture; }
    }
    public static final class Bogged extends SkeletonVariant {
        public Bogged(EntityRendererProvider.Context c) { super(c, "textures/entity/skeleton/bogged.png"); }
    }
    public static final class Parched extends SkeletonVariant {
        public Parched(EntityRendererProvider.Context c) { super(c, "textures/entity/skeleton/skeleton.png"); }
    }
    public static final class Stray extends SkeletonVariant {
        public Stray(EntityRendererProvider.Context c) { super(c, "textures/entity/skeleton/stray.png"); }
    }
    public static final class WitherSkeleton extends SkeletonVariant {
        public WitherSkeleton(EntityRendererProvider.Context c) { super(c, "textures/entity/skeleton/wither_skeleton.png"); }
    }

    public static final class CaveSpider extends SpiderRenderer<IMCaveSpiderEntity> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/spider/cave_spider.png");
        public CaveSpider(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(IMCaveSpiderEntity e) { return TEXTURE; }
    }

    public static final class Enderman extends MobRenderer<IMEndermanEntity, EndermanModel<IMEndermanEntity>> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");
        public Enderman(EntityRendererProvider.Context c) {
            super(c, new EndermanModel<>(c.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
        }
        @Override public ResourceLocation getTextureLocation(IMEndermanEntity e) { return TEXTURE; }
    }

    public static final class ZombifiedPiglin extends HumanoidMobRenderer<IMZombifiedPiglinEntity, ZombieModel<IMZombifiedPiglinEntity>> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");
        public ZombifiedPiglin(EntityRendererProvider.Context c) {
            super(c, new ZombieModel<>(c.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        }
        @Override public ResourceLocation getTextureLocation(IMZombifiedPiglinEntity e) { return TEXTURE; }
    }
}
