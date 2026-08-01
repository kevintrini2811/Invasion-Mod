package com.invasion.client.render.entity;

import java.util.List;
import com.invasion.entity.AbstractIMZombieEntity;
import com.invasion.entity.IMCaveSpiderEntity;
import com.invasion.entity.IMEndermanEntity;
import com.invasion.entity.IMZombifiedPiglinEntity;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.layers.StrayClothingLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

/** Immediate-mode renderers for vanilla-derived invasion variants on 1.21.1. */
public final class VariantMobRenderers {
    private VariantMobRenderers() {}

    private abstract static class ZombieVariant extends AbstractIMZombieEntityRenderer {
        private final List<ResourceLocation> textures;
        ZombieVariant(EntityRendererProvider.Context context, String texture,
                net.minecraft.client.model.geom.ModelLayerLocation bodyLayer) {
            super(context, bodyLayer);
            textures = List.of(new ResourceLocation(texture));
        }
        @Override protected List<ResourceLocation> getTextures() { return textures; }
        @Override protected boolean isBrute(AbstractIMZombieEntity entity) { return false; }
    }
    public static final class Husk extends ZombieVariant {
        public Husk(EntityRendererProvider.Context c) { super(c, "textures/entity/zombie/husk.png", ModelLayers.HUSK); }
    }
    public static final class Drowned extends ZombieVariant {
        public Drowned(EntityRendererProvider.Context c) {
            super(c, "textures/entity/zombie/drowned.png", ModelLayers.DROWNED);
            addLayer(new HumanoidOuterLayer(this, c, ModelLayers.DROWNED_OUTER_LAYER,
                    "textures/entity/zombie/drowned_outer_layer.png"));
        }
    }
    public static final class ZombieVillager extends ZombieVariant {
        public ZombieVillager(EntityRendererProvider.Context c) { super(c, "textures/entity/zombie_villager/zombie_villager.png", ModelLayers.ZOMBIE_VILLAGER); }
    }

    private abstract static class SkeletonVariant extends IMSkeletonEntityRenderer {
        private final ResourceLocation texture;
        SkeletonVariant(EntityRendererProvider.Context c, String texture) {
            super(c);
            this.texture = new ResourceLocation(texture);
        }
        @Override public ResourceLocation getTextureLocation(com.invasion.entity.IMSkeletonEntity e) { return texture; }
    }
    public static final class Stray extends SkeletonVariant {
        public Stray(EntityRendererProvider.Context c) {
            super(c, "textures/entity/skeleton/stray.png");
            addLayer(new StrayClothingLayer<>(this, c.getModelSet()));
        }
    }
    public static final class WitherSkeleton extends SkeletonVariant {
        public WitherSkeleton(EntityRendererProvider.Context c) { super(c, "textures/entity/skeleton/wither_skeleton.png"); }
        @Override protected void scale(com.invasion.entity.IMSkeletonEntity e, PoseStack p, float f) {
            p.scale(1.2F, 1.2F, 1.2F);
        }
    }

    public static final class CaveSpider extends SpiderRenderer<IMCaveSpiderEntity> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/spider/cave_spider.png");
        public CaveSpider(EntityRendererProvider.Context c) { super(c); }
        @Override public ResourceLocation getTextureLocation(IMCaveSpiderEntity e) { return TEXTURE; }
    }

    public static final class Enderman extends MobRenderer<IMEndermanEntity, EndermanModel<IMEndermanEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/enderman/enderman.png");
        public Enderman(EntityRendererProvider.Context c) {
            super(c, new EndermanModel<>(c.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
            addLayer(new EnderEyesLayer<>(this));
            addLayer(new EndermanCarriedBlockLayer(this,
                    c.getBlockRenderDispatcher()));
            addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<IMEndermanEntity>(
                            c.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                    new HumanoidModel<IMEndermanEntity>(
                            c.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                    c.getModelManager()));
        }
        @Override
        public void render(IMEndermanEntity entity, float yaw, float tickDelta,
                PoseStack pose, MultiBufferSource buffers, int light) {
            model.carrying = entity.isCarryingBlock();
            model.creepy = entity.isAggressive();
            super.render(entity, yaw, tickDelta, pose, buffers, light);
        }
        @Override public ResourceLocation getTextureLocation(IMEndermanEntity e) { return TEXTURE; }
    }

    private static final class EndermanCarriedBlockLayer extends RenderLayer<
            IMEndermanEntity, EndermanModel<IMEndermanEntity>> {
        private final BlockRenderDispatcher blockRenderer;

        EndermanCarriedBlockLayer(Enderman parent,
                BlockRenderDispatcher blockRenderer) {
            super(parent);
            this.blockRenderer = blockRenderer;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers,
                int light, IMEndermanEntity entity, float limbSwing,
                float limbSwingAmount, float partialTick, float age,
                float headYaw, float headPitch) {
            entity.getCarriedBlock().ifPresent(state -> {
                pose.pushPose();
                pose.translate(0.0F, 0.6875F, -0.75F);
                pose.mulPose(Axis.XP.rotationDegrees(20.0F));
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.translate(0.25F, 0.1875F, 0.25F);
                pose.scale(-0.5F, -0.5F, 0.5F);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                blockRenderer.renderSingleBlock(state, pose, buffers,
                        light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            });
        }
    }

    private static final class HumanoidOuterLayer extends RenderLayer<
            AbstractIMZombieEntity, net.minecraft.client.model.HumanoidModel<AbstractIMZombieEntity>> {
        private final net.minecraft.client.model.HumanoidModel<AbstractIMZombieEntity> model;
        private final ResourceLocation texture;
        HumanoidOuterLayer(AbstractIMZombieEntityRenderer parent, EntityRendererProvider.Context c,
                net.minecraft.client.model.geom.ModelLayerLocation layer, String texture) {
            super(parent);
            model = new net.minecraft.client.model.HumanoidModel<>(c.bakeLayer(layer));
            this.texture = new ResourceLocation(texture);
        }
        @Override public void render(PoseStack p, MultiBufferSource b, int light,
                AbstractIMZombieEntity e, float a, float d, float tick, float age, float yaw, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            getParentModel().copyPropertiesTo(model);
            model.renderToBuffer(p, b.getBuffer(RenderType.entityCutoutNoCull(texture)),
                    light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        }
    }

    public static final class ZombifiedPiglin extends HumanoidMobRenderer<
            IMZombifiedPiglinEntity, PiglinModel<IMZombifiedPiglinEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/piglin/zombified_piglin.png");
        public ZombifiedPiglin(EntityRendererProvider.Context c) {
            super(c, new PiglinModel<>(c.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN)), 0.5F);
            addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<IMZombifiedPiglinEntity>(
                            c.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR)),
                    new HumanoidModel<IMZombifiedPiglinEntity>(
                            c.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR)),
                    c.getModelManager()));
        }
        @Override public ResourceLocation getTextureLocation(IMZombifiedPiglinEntity e) { return TEXTURE; }
    }
}
