package com.invasion.client.render.legacy;

import com.invasion.entity.InvEntities;
import com.invasion.client.render.IMZoglinRenderer;
import com.invasion.client.render.entity.AbstractIMZombieEntityRenderer;
import com.invasion.client.render.entity.BoulderEntityRenderer;
import com.invasion.client.render.entity.BurrowerEntityRenderer;
import com.invasion.client.render.entity.ElectricityBoltEntityRenderer;
import com.invasion.client.render.entity.IMCreeperEntityRenderer;
import com.invasion.client.render.entity.IMBlazeRenderer;
import com.invasion.client.render.entity.IMGhastRenderer;
import com.invasion.client.render.entity.IMSkeletonEntityRenderer;
import com.invasion.client.render.entity.IMSpiderEntityRenderer;
import com.invasion.client.render.entity.IMWolfEntityRenderer;
import com.invasion.client.render.entity.ImpEntityRenderer;
import com.invasion.client.render.entity.PigmanEngineerEntityRenderer;
import com.invasion.client.render.entity.ZombieBuilderEntityRenderer;
import com.invasion.client.render.entity.ZombieMinerEntityRenderer;
import com.invasion.client.render.entity.ThrowerEntityRenderer;
import com.invasion.client.render.entity.TntEntityRenderer;
import com.invasion.client.render.entity.SpiderEggEntityRenderer;
import com.invasion.client.render.entity.SpeedyZombieEntityRenderer;
import com.invasion.client.render.entity.SkeletonArrowEntityRenderer;
import com.invasion.client.render.entity.TrapEntityRenderer;
import com.invasion.client.render.entity.ZombiePigmanEntityRenderer;
import com.invasion.client.render.entity.VariantMobRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.WitherBossRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class InvRenderers {
    private InvRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InvEntities.ZOMBIE, AbstractIMZombieEntityRenderer::new);
        event.registerEntityRenderer(
                InvEntities.SPEEDY_ZOMBIE,
                SpeedyZombieEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.HUSK, VariantMobRenderers.Husk::new);
        event.registerEntityRenderer(InvEntities.DROWNED, VariantMobRenderers.Drowned::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_VILLAGER, VariantMobRenderers.ZombieVillager::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_PIGMAN, ZombiePigmanEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOGLIN, IMZoglinRenderer::new);
        event.registerEntityRenderer(InvEntities.WITHER, WitherBossRenderer::new);
        event.registerEntityRenderer(InvEntities.WARDEN, WardenRenderer::new);
        event.registerEntityRenderer(
                InvEntities.WITHER_SKULL, WitherSkullRenderer::new);
        event.registerEntityRenderer(InvEntities.WITCH, WitchRenderer::new);
        event.registerEntityRenderer(InvEntities.GHAST, IMGhastRenderer::new);
        event.registerEntityRenderer(
                InvEntities.WITCH_POTION, ThrownItemRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOMBIFIED_PIGLIN, VariantMobRenderers.ZombifiedPiglin::new);
        event.registerEntityRenderer(InvEntities.SKELETON, IMSkeletonEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BOGGED, VariantMobRenderers.Bogged::new);
        event.registerEntityRenderer(InvEntities.STRAY, VariantMobRenderers.Stray::new);
        event.registerEntityRenderer(InvEntities.WITHER_SKELETON, VariantMobRenderers.WitherSkeleton::new);
        event.registerEntityRenderer(InvEntities.SPIDER,
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.NORMAL));
        event.registerEntityRenderer(InvEntities.JUMPING_SPIDER,
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.JUMPER));
        event.registerEntityRenderer(InvEntities.CAVE_SPIDER,
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.CAVE));
        event.registerEntityRenderer(InvEntities.QUEEN_SPIDER,
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.MOTHER));
        event.registerEntityRenderer(InvEntities.PIGMAN_ENGINEER, PigmanEngineerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_BUILDER, ZombieBuilderEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_MINER, ZombieMinerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.IMP, ImpEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BLAZE, IMBlazeRenderer::new);
        event.registerEntityRenderer(InvEntities.BREEZE, BreezeRenderer::new);
        event.registerEntityRenderer(InvEntities.SILVERFISH, SilverfishRenderer::new);
        event.registerEntityRenderer(InvEntities.ENDERMITE, EndermiteRenderer::new);
        event.registerEntityRenderer(InvEntities.SLIME, SlimeRenderer::new);
        event.registerEntityRenderer(
                InvEntities.MAGMA_CUBE, MagmaCubeRenderer::new);
        event.registerEntityRenderer(InvEntities.ENDERMAN, VariantMobRenderers.Enderman::new);
        event.registerEntityRenderer(InvEntities.PHANTOM, PhantomRenderer::new);
        event.registerEntityRenderer(InvEntities.THROWER, ThrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER, BurrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER_TAIL, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.BOULDER, BoulderEntityRenderer::new);
        event.registerEntityRenderer(
                InvEntities.SKELETON_ARROW, SkeletonArrowEntityRenderer::new);
        event.registerEntityRenderer(
                InvEntities.THROWN_ITEM, ThrownItemRenderer::new);
        event.registerEntityRenderer(InvEntities.TNT, TntEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.WOLF, IMWolfEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.TRAP, TrapEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BOLT, ElectricityBoltEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SFX, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPAWN_PROXY, NoopRenderer::new);
        event.registerEntityRenderer(
                InvEntities.SPIDER_EGG, SpiderEggEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.CREEPER, IMCreeperEntityRenderer::new);
    }
}
