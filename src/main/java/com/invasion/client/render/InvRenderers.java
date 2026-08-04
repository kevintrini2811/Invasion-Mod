package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.InvEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers a safe renderer for every custom entity while the legacy
 * immediate-mode renderers are migrated to Minecraft 26.2's render-state API.
 */
public final class InvRenderers {
    private InvRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InvEntities.ZOMBIE,
                context -> new InvasionZombieRenderer<>(context, false));
        event.registerEntityRenderer(InvEntities.HUSK, InvHuskRenderer::new);
        event.registerEntityRenderer(InvEntities.DROWNED, InvDrownedRenderer::new);
        event.registerEntityRenderer(
                InvEntities.ZOMBIE_VILLAGER,
                IMZombieVillagerRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_PIGMAN,
                context -> new InvasionZombieRenderer<>(context, true));
        event.registerEntityRenderer(InvEntities.ZOGLIN, IMZoglinRenderer::new);
        event.registerEntityRenderer(InvEntities.WITHER, WitherBossRenderer::new);
        event.registerEntityRenderer(InvEntities.WARDEN, WardenRenderer::new);
        event.registerEntityRenderer(
                InvEntities.WITHER_SKULL, WitherSkullRenderer::new);
        event.registerEntityRenderer(InvEntities.WITCH, WitchRenderer::new);
        event.registerEntityRenderer(
                InvEntities.WITCH_POTION, ThrownItemRenderer::new);
        event.registerEntityRenderer(InvEntities.SKELETON, InvSkeletonRenderer::new);
        event.registerEntityRenderer(InvEntities.PARCHED, InvParchedRenderer::new);
        event.registerEntityRenderer(InvEntities.STRAY, InvStrayRenderer::new);
        event.registerEntityRenderer(
                InvEntities.WITHER_SKELETON,
                InvWitherSkeletonRenderer::new);
        event.registerEntityRenderer(InvEntities.SPIDER,
                context -> new TexturedSpiderRenderer<>(context,
                        new ResourceLocation("textures/entity/spider/spider.png")));
        event.registerEntityRenderer(InvEntities.JUMPING_SPIDER,
                context -> new TexturedSpiderRenderer<>(context, texture("entity/spider/jumping_spider.png")));
        event.registerEntityRenderer(InvEntities.CAVE_SPIDER,
                context -> new TexturedSpiderRenderer<>(context,
                        new ResourceLocation("textures/entity/spider/cave_spider.png")));
        event.registerEntityRenderer(InvEntities.QUEEN_SPIDER,
                context -> new TexturedSpiderRenderer<>(context, texture("entity/spider/mother_spider.png")));
        event.registerEntityRenderer(InvEntities.PIGMAN_ENGINEER,
                context -> new GenericHumanoidMobRenderer<>(context, texture("entity/pigman_engineer.png"), 0.5F));
        event.registerEntityRenderer(InvEntities.IMP, ImpRenderer::new);
        event.registerEntityRenderer(InvEntities.BLAZE, BlazeRenderer::new);
        event.registerEntityRenderer(InvEntities.SILVERFISH, SilverfishRenderer::new);
        event.registerEntityRenderer(InvEntities.ENDERMITE, EndermiteRenderer::new);
        event.registerEntityRenderer(InvEntities.SLIME, SlimeRenderer::new);
        event.registerEntityRenderer(
                InvEntities.MAGMA_CUBE, MagmaCubeRenderer::new);
        event.registerEntityRenderer(InvEntities.ENDERMAN, IMEndermanRenderer::new);
        event.registerEntityRenderer(InvEntities.PHANTOM, PhantomRenderer::new);
        event.registerEntityRenderer(
                InvEntities.ZOMBIFIED_PIGLIN,
                context -> new ZombifiedPiglinRenderer(
                        context,
                        ModelLayers.ZOMBIFIED_PIGLIN,
                        ModelLayers.ZOMBIFIED_PIGLIN_BABY,
                        ModelLayers.ZOMBIFIED_PIGLIN_ARMOR,
                        ModelLayers.ZOMBIFIED_PIGLIN_BABY_ARMOR));
        event.registerEntityRenderer(InvEntities.THROWER, ThrowerRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER, BurrowerRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER_TAIL, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.BOULDER, BoulderProjectileRenderer::new);
        event.registerEntityRenderer(InvEntities.SKELETON_ARROW, SkeletonArrowRenderer::new);
        event.registerEntityRenderer(InvEntities.TNT, TntProjectileRenderer::new);
        event.registerEntityRenderer(InvEntities.WOLF,
                IMWolfRenderer::new);
        event.registerEntityRenderer(InvEntities.TRAP, TrapRenderer::new);
        event.registerEntityRenderer(InvEntities.BOLT, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SFX, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPAWN_PROXY,
                context -> new GenericHumanoidMobRenderer<>(context, texture("entity/test.png"), 0.25F));
        event.registerEntityRenderer(InvEntities.SPIDER_EGG, SpiderEggEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.CREEPER, GenericCreeperRenderer::new);
    }

    private static ResourceLocation texture(String path) {
        return InvasionMod.id("textures/" + path);
    }

    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation(path);
    }
}
