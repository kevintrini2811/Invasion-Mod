package com.invasion.client.render;

import com.invasion.InvasionMod;
import com.invasion.entity.InvEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.Identifier;

/**
 * Registers a safe renderer for every custom entity while the legacy
 * immediate-mode renderers are migrated to Minecraft 26.2's render-state API.
 */
public final class InvRenderers {
    private InvRenderers() {
    }

    public static void bootstrap() {
        EntityRendererRegistry.register(InvEntities.ZOMBIE,
                context -> new InvasionZombieRenderer<>(context, false));
        EntityRendererRegistry.register(
                InvEntities.SPEEDY_ZOMBIE,
                SpeedyZombieRenderer::new);
        EntityRendererRegistry.register(InvEntities.HUSK, InvHuskRenderer::new);
        EntityRendererRegistry.register(InvEntities.DROWNED, InvDrownedRenderer::new);
        EntityRendererRegistry.register(
                InvEntities.ZOMBIE_VILLAGER,
                IMZombieVillagerRenderer::new);
        EntityRendererRegistry.register(InvEntities.ZOMBIE_PIGMAN,
                context -> new InvasionZombieRenderer<>(context, true));
        EntityRendererRegistry.register(InvEntities.ZOGLIN, IMZoglinRenderer::new);
        EntityRendererRegistry.register(InvEntities.WITHER, WitherBossRenderer::new);
        EntityRendererRegistry.register(
                InvEntities.WITHER_SKULL, WitherSkullRenderer::new);
        EntityRendererRegistry.register(InvEntities.WITCH, WitchRenderer::new);
        EntityRendererRegistry.register(
                InvEntities.WITCH_POTION, ThrownItemRenderer::new);
        EntityRendererRegistry.register(InvEntities.SKELETON, InvSkeletonRenderer::new);
        EntityRendererRegistry.register(InvEntities.BOGGED, InvBoggedRenderer::new);
        EntityRendererRegistry.register(InvEntities.PARCHED, InvParchedRenderer::new);
        EntityRendererRegistry.register(InvEntities.STRAY, InvStrayRenderer::new);
        EntityRendererRegistry.register(
                InvEntities.WITHER_SKELETON,
                InvWitherSkeletonRenderer::new);
        EntityRendererRegistry.register(InvEntities.SPIDER,
                context -> new TexturedSpiderRenderer<>(context,
                        Identifier.withDefaultNamespace("textures/entity/spider/spider.png")));
        EntityRendererRegistry.register(InvEntities.JUMPING_SPIDER,
                context -> new TexturedSpiderRenderer<>(context, texture("entity/spider/jumping_spider.png")));
        EntityRendererRegistry.register(InvEntities.CAVE_SPIDER,
                context -> new TexturedSpiderRenderer<>(context,
                        Identifier.withDefaultNamespace("textures/entity/spider/cave_spider.png")));
        EntityRendererRegistry.register(InvEntities.QUEEN_SPIDER,
                context -> new TexturedSpiderRenderer<>(context, texture("entity/spider/mother_spider.png")));
        EntityRendererRegistry.register(InvEntities.PIGMAN_ENGINEER,
                context -> new GenericHumanoidMobRenderer<>(context, texture("entity/pigman_engineer.png"), 0.5F));
        EntityRendererRegistry.register(InvEntities.IMP, ImpRenderer::new);
        EntityRendererRegistry.register(InvEntities.BLAZE, BlazeRenderer::new);
        EntityRendererRegistry.register(InvEntities.SILVERFISH, SilverfishRenderer::new);
        EntityRendererRegistry.register(InvEntities.ENDERMAN, IMEndermanRenderer::new);
        EntityRendererRegistry.register(InvEntities.PHANTOM, PhantomRenderer::new);
        EntityRendererRegistry.register(
                InvEntities.ZOMBIFIED_PIGLIN,
                context -> new ZombifiedPiglinRenderer(
                        context,
                        ModelLayers.ZOMBIFIED_PIGLIN,
                        ModelLayers.ZOMBIFIED_PIGLIN_BABY,
                        ModelLayers.ZOMBIFIED_PIGLIN_ARMOR,
                        ModelLayers.ZOMBIFIED_PIGLIN_BABY_ARMOR));
        EntityRendererRegistry.register(InvEntities.THROWER, ThrowerRenderer::new);
        EntityRendererRegistry.register(InvEntities.BURROWER, BurrowerRenderer::new);
        EntityRendererRegistry.register(InvEntities.BURROWER_TAIL, NoopRenderer::new);
        EntityRendererRegistry.register(InvEntities.BOULDER, BoulderProjectileRenderer::new);
        EntityRendererRegistry.register(InvEntities.SKELETON_ARROW, SkeletonArrowRenderer::new);
        EntityRendererRegistry.register(InvEntities.TNT, TntProjectileRenderer::new);
        EntityRendererRegistry.register(InvEntities.WOLF,
                IMWolfRenderer::new);
        EntityRendererRegistry.register(InvEntities.TRAP, TrapRenderer::new);
        EntityRendererRegistry.register(InvEntities.BOLT, NoopRenderer::new);
        EntityRendererRegistry.register(InvEntities.SFX, NoopRenderer::new);
        EntityRendererRegistry.register(InvEntities.SPAWN_PROXY,
                context -> new GenericHumanoidMobRenderer<>(context, texture("entity/test.png"), 0.25F));
        EntityRendererRegistry.register(InvEntities.SPIDER_EGG, SpiderEggEntityRenderer::new);
        EntityRendererRegistry.register(InvEntities.CREEPER, GenericCreeperRenderer::new);
    }

    private static Identifier texture(String path) {
        return InvasionMod.id("textures/" + path);
    }

    private static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }
}
