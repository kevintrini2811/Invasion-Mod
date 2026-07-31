package com.invasion.client.render;

import com.invasion.entity.InvEntities;
import com.invasion.client.render.entity.AbstractIMZombieEntityRenderer;
import com.invasion.client.render.entity.BoulderEntityRenderer;
import com.invasion.client.render.entity.BurrowerEntityRenderer;
import com.invasion.client.render.entity.ElectricityBoltEntityRenderer;
import com.invasion.client.render.entity.IMCreeperEntityRenderer;
import com.invasion.client.render.entity.IMSkeletonEntityRenderer;
import com.invasion.client.render.entity.IMSpiderEntityRenderer;
import com.invasion.client.render.entity.IMWolfEntityRenderer;
import com.invasion.client.render.entity.ImpEntityRenderer;
import com.invasion.client.render.entity.PigmanEngineerEntityRenderer;
import com.invasion.client.render.entity.ThrowerEntityRenderer;
import com.invasion.client.render.entity.TntEntityRenderer;
import com.invasion.client.render.entity.TrapEntityRenderer;
import com.invasion.client.render.entity.ZombiePigmanEntityRenderer;
import com.invasion.client.render.entity.VariantMobRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class InvRenderers {
    private InvRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InvEntities.ZOMBIE, AbstractIMZombieEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.HUSK, VariantMobRenderers.Husk::new);
        event.registerEntityRenderer(InvEntities.DROWNED, VariantMobRenderers.Drowned::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_VILLAGER, VariantMobRenderers.ZombieVillager::new);
        event.registerEntityRenderer(InvEntities.ZOMBIE_PIGMAN, ZombiePigmanEntityRenderer::new);
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
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.CAVE, 0.7F));
        event.registerEntityRenderer(InvEntities.QUEEN_SPIDER,
                context -> new IMSpiderEntityRenderer<>(context, IMSpiderEntityRenderer.MOTHER, 2.0F));
        event.registerEntityRenderer(InvEntities.PIGMAN_ENGINEER, PigmanEngineerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.IMP, ImpEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.ENDERMAN, VariantMobRenderers.Enderman::new);
        event.registerEntityRenderer(InvEntities.PHANTOM, PhantomRenderer::new);
        event.registerEntityRenderer(InvEntities.THROWER, ThrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER, BurrowerEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BURROWER_TAIL, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.BOULDER, BoulderEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SKELETON_ARROW, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.TNT, TntEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.WOLF, IMWolfEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.TRAP, TrapEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.BOLT, ElectricityBoltEntityRenderer::new);
        event.registerEntityRenderer(InvEntities.SFX, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPAWN_PROXY, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.SPIDER_EGG, NoopRenderer::new);
        event.registerEntityRenderer(InvEntities.CREEPER, IMCreeperEntityRenderer::new);
    }
}
